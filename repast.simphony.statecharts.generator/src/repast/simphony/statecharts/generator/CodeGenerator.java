/**
 * 
 */
package repast.simphony.statecharts.generator;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EcorePackage;
import org.eclipse.emf.ecore.xmi.impl.XMIResourceImpl;
import org.eclipse.gmf.runtime.notation.NotationPackage;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.xpand2.XpandExecutionContextImpl;
import org.eclipse.xpand2.XpandFacade;
import org.eclipse.xpand2.output.Outlet;
import org.eclipse.xpand2.output.Output;
import org.eclipse.xpand2.output.OutputImpl;
import org.eclipse.xtend.expression.Variable;
import org.eclipse.xtend.typesystem.emf.EmfRegistryMetaModel;

import repast.simphony.eclipse.util.DirectoryCleaner;
import repast.simphony.statecharts.scmodel.StateMachine;
import repast.simphony.statecharts.scmodel.StatechartPackage;

/**
 * Generates java / groovy code from the statechart diagram.
 * 
 * @author Nick Collier
 */
public class CodeGenerator {

  private GeneratorRecord genRec = new GeneratorRecord();

  /**
   * Gets the GeneratorRecord for this CodeGenerator.
   * 
   * @return the GeneratorRecord for this CodeGenerator.
   */
  public GeneratorRecord getGeneratorRecord() {
    return genRec;
  }

  /**
   * Generates the code in the specified project from a diagram in the specified
   * path. The code will be generated in a src-gen directory in the specified
   * project. If src-gen is not on the project's classpath it will be added.
   * 
   * @param project
   *          the project to generate the code into
   * @param path
   *          the path to the diagram file
   * @param monitor
   * @throws CoreException
   */
  public IPath run(IProject project, IPath path, IProgressMonitor monitor) throws CoreException {
    try {
      XMIResourceImpl resource = new XMIResourceImpl();
      resource.load(new FileInputStream(path.toFile()), new HashMap<Object, Object>());

      StateMachine statemachine = null;
      for (EObject obj : resource.getContents()) {
        if (obj.eClass().equals(StatechartPackage.Literals.STATE_MACHINE)) {
          statemachine = (StateMachine) obj;
          break;
        }
      }

      // don't continue the code generation when there machine is missing
      // properties that will cause the generation to fail badly (e.g. there is
      // no class name so we can't construct a file name to write the code to).
      if (new StateMachineValidator().validate(statemachine).getSeverity() == IStatus.ERROR)
        return null;
      genRec.addUUID(statemachine.getUuid());

      IPath srcPath = addSrcPath(project, statemachine, monitor);
      IPath projectLocation = project.getLocation();
      srcPath = projectLocation.append(srcPath.lastSegment());

      Output output = new OutputImpl();
      Outlet outlet = new Outlet(srcPath.toPortableString());
      outlet.setOverwrite(true);
      outlet.addPostprocessor(new CodeBeautifier());
      output.addOutlet(outlet);

      Map<String, Variable> varsMap = new HashMap<String, Variable>();
      XpandExecutionContextImpl execCtx = new XpandExecutionContextImpl(output, null, varsMap,
          null, null);
      EmfRegistryMetaModel metamodel = new EmfRegistryMetaModel() {
        @Override
        protected EPackage[] allPackages() {
          return new EPackage[] { StatechartPackage.eINSTANCE, EcorePackage.eINSTANCE,
              NotationPackage.eINSTANCE };
        }
      };
      execCtx.registerMetaModel(metamodel);

      // generate
      XpandFacade facade = XpandFacade.create(execCtx);
      String templatePath = "src::generator::Main";
      facade.evaluate(templatePath, statemachine);

      return srcPath;
    } catch (IOException ex) {
      ex.printStackTrace();
      return null;
    }

  }

  private IPath addSrcPath(IProject project, StateMachine statemachine, IProgressMonitor monitor)
      throws CoreException {
    IJavaProject javaProject = JavaCore.create(project);
    // workspace relative
    IPath srcPath = javaProject.getPath().append(CodeGeneratorConstants.SRC_GEN + "/");
    // project relative
    IFolder folder = project.getFolder(CodeGeneratorConstants.SRC_GEN);

    if (!folder.exists()) {
      PathUtils.createSrcPath(project, CodeGeneratorConstants.SRC_GEN, monitor);
    } else {
      String svgPath = CodeGeneratorConstants.SRC_GEN + "/"
          + ((statemachine.getPackage() + "." + statemachine.getClassName()).replace(".", "/"))
          + ".svg";
      genRec.addSVG(new Path(project.getFullPath().toPortableString()).append(svgPath));
      CodeGenFilter filter = new CodeGenFilter(svgPath, statemachine.getUuid());
      DirectoryCleaner cleaner = new DirectoryCleaner(filter);
      // System.out.println("running cleaner on: " +
      // project.getLocation().append(srcPath.lastSegment()).append(pkg.replace(".",
      // "/")).toPortableString());

      cleaner.run(project.getLocation().append(srcPath.lastSegment()).toPortableString());
    }

    return srcPath;
  }
}
