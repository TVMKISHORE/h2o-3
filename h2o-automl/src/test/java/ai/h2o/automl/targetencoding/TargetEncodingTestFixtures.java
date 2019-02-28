package ai.h2o.automl.targetencoding;

import ai.h2o.automl.Algo;
import hex.Model;
import hex.ModelBuilder;
import hex.ScoreKeeper;
import hex.genmodel.utils.DistributionFamily;
import hex.tree.SharedTreeModel;
import hex.tree.gbm.GBM;
import hex.tree.gbm.GBMModel;
import water.Job;
import water.Key;
import water.fvec.Frame;

import java.util.Random;

public class TargetEncodingTestFixtures {
  public TargetEncodingTestFixtures() {

  }

  public static TargetEncodingParams defaultTEParams() {
    return new TargetEncodingParams(new BlendingParams(3, 1), TargetEncoder.DataLeakageHandlingStrategy.KFold, 0.01);
  }
  public static TargetEncodingParams randomTEParams(long seed) {
    Random generator = seed == -1 ? new Random() : new Random(seed);
    double pivot = generator.nextDouble();
    byte strategy = pivot >= 0.5 ? TargetEncoder.DataLeakageHandlingStrategy.KFold : TargetEncoder.DataLeakageHandlingStrategy.LeaveOneOut;
    return new TargetEncodingParams(new BlendingParams(3, 1), strategy, 0.01);
  }
  
  public static TargetEncodingParams randomTEParams() {
    return randomTEParams(-1);
  }

  // TODO use this method and remove the one in the test
  public static ModelBuilder modelBuilderWithCVFixture(Frame fr, String responseColumnName , long builderSeed) {
    Algo algo = Algo.GBM;
    String algoUrlName = algo.name().toLowerCase();
    String algoName = ModelBuilder.algoName(algoUrlName);
    Key<Model> testModelKey = Key.make("testModelKey");

    Job<Model> job = new Job<>(testModelKey, ModelBuilder.javaName(algoUrlName), algoName);
    ModelBuilder builder = ModelBuilder.make(algoUrlName, job, testModelKey);

    // Model Parameters
    GBMModel.GBMParameters gbmParameters = new GBMModel.GBMParameters();
    gbmParameters._score_tree_interval = 5;
    gbmParameters._histogram_type = SharedTreeModel.SharedTreeParameters.HistogramType.AUTO;

    builder._parms = gbmParameters;
    builder._parms._seed = builderSeed;

    builder._parms._train = fr._key;
    builder._parms._response_column = responseColumnName;
    builder._parms._nfolds = 5;
    return builder;
  }
  
  // TODO use this method and remove the one in the test
  public static ModelBuilder modelBuilderWithValidFrameFixture(Frame fr, String responseColumnName , long builderSeed) {
    Algo algo = Algo.GBM;
    String algoUrlName = algo.name().toLowerCase();
    String algoName = ModelBuilder.algoName(algoUrlName);
    Key<Model> testModelKey = Key.make("testModelKey");

    Job<Model> job = new Job<>(testModelKey, ModelBuilder.javaName(algoUrlName), algoName);
    ModelBuilder builder = ModelBuilder.make(algoUrlName, job, testModelKey);

    // Model Parameters
    GBMModel.GBMParameters gbmParameters = new GBMModel.GBMParameters();
    gbmParameters._score_tree_interval = 5;
    gbmParameters._histogram_type = SharedTreeModel.SharedTreeParameters.HistogramType.AUTO;

    builder._parms = gbmParameters;
    builder._parms._seed = builderSeed;

    builder._parms._train = fr._key;
    builder._parms._response_column = responseColumnName;

    return builder;
  }

  static GBMModel trainGBM(Frame inputData, String responseColumn, String[] columnsToExclude)  {
    GBMModel gbm = null; // TODO maybe we should receive GBMParameters from caller?

    try {
      GBMModel.GBMParameters parms = new GBMModel.GBMParameters();
      parms._train = inputData._key;
      parms._response_column = responseColumn;
      parms._score_tree_interval = 10;
      parms._ntrees = 100;
//      parms._fold_column = foldColumnName; //TODO we will train on differend folds that will be assigned by the models themselves. Fold column that was used for TE should be excluded.
      parms._nfolds = 5;
      parms._max_depth = 5;
      parms._distribution = DistributionFamily.multinomial;
      parms._stopping_tolerance = 0.001;
      parms._stopping_metric = ScoreKeeper.StoppingMetric.AUC;
      parms._stopping_rounds = 5;
      parms._ignored_columns = columnsToExclude;
      parms._keep_cross_validation_fold_assignment = false;
      parms._keep_cross_validation_models = false;
      parms._seed = 1234L;
      GBM job = new GBM(parms);
      gbm = job.trainModel().get();
    } finally {
      //      System.out.println(gbm._output._variable_importances.toString(2, true));
    }
    return gbm;
  }

}
