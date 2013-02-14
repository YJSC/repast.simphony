package repast.simphony.ui.probe;

import java.beans.PropertyDescriptor;

import javax.swing.JComponent;

import com.jgoodies.binding.PresentationModel;

/**
 * Represents a property of a probed object whether is read / read only etc.
 * Also produces a JComponent to display and edit the property.
 * 
 * @author Nick Collier
 * @version $Revision$ $Date$
 */
public abstract class AbstractProbedProperty {

  protected enum Type {
    READ_WRITE, WRITE, READ
  };

  protected String displayName;
  protected String name;
  protected Type type;

  protected AbstractProbedProperty(PropertyDescriptor desc) {
    name = desc.getName();
    displayName = desc.getDisplayName();
    if (desc.getReadMethod() != null && desc.getWriteMethod() != null) {
      type = Type.READ_WRITE;
    } else if (desc.getWriteMethod() == null) {
      type = Type.READ;
    } else {
      type = Type.WRITE;
    }
  }

  public abstract JComponent getComponent(PresentationModel model, boolean buffered);

  public String getName() {
    return name;
  }

  public String getDisplayName() {
    return displayName;
  }

  public void printValue(PresentationModel model) {
    System.out.println(name + " = " + model.getValue(name));
  }
}
