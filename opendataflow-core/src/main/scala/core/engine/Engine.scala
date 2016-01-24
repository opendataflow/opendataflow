package core.engine

import com.typesafe.config.{ ConfigFactory, Config }
import core.Pipeline

/**
 * Created by OpenDataFlow on 02/01/16.
 */
abstract class Engine(val config: Config) {
  def createJob(p: Pipeline, jobConfig: Config = ConfigFactory.empty()): PipelineJob
  def submit(pj: PipelineJob): JobResult
}
