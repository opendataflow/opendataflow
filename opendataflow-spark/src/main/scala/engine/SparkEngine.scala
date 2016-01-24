package engine

import com.typesafe.config.Config
import core.Pipeline
import core.engine.{ JobResult, PipelineJob, Engine }

/**
 * Created by OpenDataFlow on 02/01/16.
 */
class SparkEngine(val c: Config) extends Engine(c) {
  override def createJob(p: Pipeline, jobConfig: Config): PipelineJob = ???
  override def submit(pj: PipelineJob): JobResult = ???
}
