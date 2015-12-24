import core.{ OutputConnector, InputConnector, PComponent }
import core.data.BoundData

class WordCountPComponent extends PComponent {

  addConnector(InputConnector("source", new BoundData(), "input text"))
  addConnector(OutputConnector("sink", new BoundData(), "word count for each word"))

  /**
   * run method is the one that actually performs the work
   */
  override def run: Unit = ???
}

class WordCountWithStopWordsPComponent extends PComponent {

  addConnector(InputConnector("source", new BoundData(), "input text"))
  addConnector(OutputConnector("sink", new BoundData(), "word count for each word"))
  addConnector(InputConnector("stopwords", new BoundData(), "words we want to exclude"))

  /**
   * run method is the one that actually performs the work
   */
  override def run: Unit = ???
}