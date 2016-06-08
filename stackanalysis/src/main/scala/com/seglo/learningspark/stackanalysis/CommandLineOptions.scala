package com.seglo.learningspark.stackanalysis

// library-less & functional command line option parser
// http://stackoverflow.com/a/3183991/895309

object CommandLineOptions {
  val usage = """
  Usage: learning-spark --input-file hdfsfilenam
  """
  def getMap(args: Array[String]) = {
    if (args.length == 0) println(usage)
    val arglist = args.toList
    type OptionMap = Map[Symbol, Any]

    def nextOption(map : OptionMap, list: List[String]) : OptionMap = {
      def isSwitch(s : String) = (s(0) == '-')

      list match {
        case Nil ⇒ map
        case "--input-file" :: value :: tail ⇒
          nextOption(map ++ Map('inputfile -> value), tail)
        case "--output-directory" :: value :: tail ⇒
          nextOption(map ++ Map('outputdir -> value), tail)
        case option :: tail ⇒ println("Unknown option "+option)
          sys.exit(1)
      }
    }
    val options = nextOption(Map(),arglist)
    println(options)
    options
  }
}
