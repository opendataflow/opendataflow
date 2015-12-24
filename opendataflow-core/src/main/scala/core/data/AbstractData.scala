package core.data

/**
 * Hierarchy for Abstract Data Descriptors.
 *
 * When the pipeline is passed to the runner,
 * the runner will create the appropriate
 * delegate depending on the engine.
 * In Spark, UnboundData would be a DStream
 * and BoundData would be DataFrames.
 */
abstract class AbstractData {

}

class BoundData extends AbstractData {

}

class OffsetBasedData extends BoundData {

}

class FileBasedData extends OffsetBasedData {

}

class BlockBasedData extends FileBasedData {

}

class UnboundData extends AbstractData {

}
