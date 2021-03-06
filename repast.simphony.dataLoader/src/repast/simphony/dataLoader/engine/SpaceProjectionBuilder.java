package repast.simphony.dataLoader.engine;

import static repast.simphony.dataLoader.engine.AutoBuilderConstants.BORDER_RULE_ID;
import static repast.simphony.dataLoader.engine.AutoBuilderConstants.BOUNCY_RULE;
import static repast.simphony.dataLoader.engine.AutoBuilderConstants.PERIODIC_RULE;
import static repast.simphony.dataLoader.engine.AutoBuilderConstants.STICKY_RULE;
import static repast.simphony.dataLoader.engine.AutoBuilderConstants.STRICT_RULE;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import repast.simphony.context.Context;
import repast.simphony.context.space.continuous.ContinuousSpaceFactory;
import repast.simphony.context.space.continuous.ContinuousSpaceFactoryFinder;
import repast.simphony.dataLoader.ContextBuilder;
import repast.simphony.engine.environment.RunEnvironment;
import repast.simphony.parameter.Parameters;
import repast.simphony.scenario.data.Attribute;
import repast.simphony.scenario.data.ProjectionData;
import repast.simphony.space.continuous.BouncyBorders;
import repast.simphony.space.continuous.PointTranslator;
import repast.simphony.space.continuous.RandomCartesianAdder;
import repast.simphony.space.continuous.StickyBorders;
import repast.simphony.space.continuous.StrictBorders;
import repast.simphony.space.continuous.WrapAroundBorders;
import simphony.util.messages.MessageCenter;

/**
 * Builds a grid projection based on SContinousSpace data.
 * 
 * @author Nick Collier
 */
public class SpaceProjectionBuilder implements ProjectionBuilderFactory {

  private static final MessageCenter msg = MessageCenter
      .getMessageCenter(SpaceProjectionBuilder.class);

  private static class CSBuilder implements ContextBuilder {

    private String name, borderRule;
    private List<String> dimNames = new ArrayList<String>();
    private ProjectionData space;

    CSBuilder(ProjectionData space) {
      name = space.getId();
      this.space = space;
      init();
    }

    private void init() {
      for (Attribute attrib : space.attributes()) {
        if (attrib.getType().equals(int.class) || attrib.getType().equals(double.class)) {
          dimNames.add(space.getId() + attrib.getId());
        }

        if (attrib.getId().equalsIgnoreCase(BORDER_RULE_ID)) {
          borderRule = attrib.getValue();
        }
      }

      if (dimNames.isEmpty()) {
        String msg = "Unable to build continuous space '" + space.getId() + "': too few dimensions specified";
        SpaceProjectionBuilder.msg.error(msg, new IllegalArgumentException("Too few dimensions specified"));
      }
      
      if (borderRule == null) {
        String msg = "Unable to build continuous space '" + space.getId() + "': invalid border rule";
       SpaceProjectionBuilder.msg.error(msg, new IllegalArgumentException("Invalid border rule"));
        
      }
    }

    public Context build(Context context) {
      ContinuousSpaceFactory factory = ContinuousSpaceFactoryFinder.createContinuousSpaceFactory(new HashMap<String, Object>());
      double[] dims = new double[dimNames.size()];
      Parameters p = RunEnvironment.getInstance().getParameters();
      for (int i = 0; i < dimNames.size(); i++) {
        dims[i] = ((Number) p.getValue(dimNames.get(i))).doubleValue();
      }

      factory.createContinuousSpace(name, context, new RandomCartesianAdder(), translatorFor(borderRule), dims);
      return context;
    }

    private PointTranslator translatorFor(String rule) {
      if (rule.equalsIgnoreCase(BOUNCY_RULE)) {
        return new BouncyBorders();
      } else if (rule.equalsIgnoreCase(STICKY_RULE)) {
        return new StickyBorders();
      } else if (rule.equalsIgnoreCase(STRICT_RULE)) {
        return new StrictBorders();
      } else if (rule.equalsIgnoreCase(PERIODIC_RULE)) {
        return new WrapAroundBorders();
      } else {
        return null;
      }
    }
  }

  /**
   * Gets a ContextBuilder to build the specified Projection.
   * 
   * @param proj
   *          the type of Projection to build
   * @return a ContextBuilder to build the specified Projection.
   */
  public ContextBuilder getBuilder(ProjectionData proj) {
    return new CSBuilder(proj);
  }

}