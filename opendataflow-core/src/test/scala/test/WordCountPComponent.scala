package test

import core.data.BoundData
import core.{ InputConnector, OutputConnector, PComponent }

class WordCountPComponent extends PComponent {
  addConnector(OutputConnector(PComponent.DEFAULT_SOURCE_STRING, new BoundData(), "word count for each word"))
  addConnector(InputConnector(PComponent.DEFAULT_SINK_STRING, new BoundData(), "input text"))

  /**
   * run method is the one that actually performs the work
   */
  override def run: Unit = ???
}

class WordCountWithStopWordsPComponent extends PComponent {
  addConnectors(
    InputConnector("source", new BoundData(), "input text"),
    InputConnector("sink", new BoundData(), "word count for each word"),
    InputConnector("stopwords", new BoundData(), "words we want to exclude"))

  /**
   * run method is the one that actually performs the work
   */
  override def run: Unit = {

  }
}