package com.seglo.learningspark.githubstream.util

import java.io.File

import org.apache.avro.Schema
import org.apache.avro.file.{DataFileReader, DataFileWriter}
import org.apache.avro.generic.{GenericData, GenericDatumReader, GenericDatumWriter, GenericRecord}

class TestAvroSerializer() {
  def write(records: Seq[GenericData.Record], filename: String, schema: Schema) = {
    // Serialize data to disk
    val file = new File(filename)

    val datumWriter = new GenericDatumWriter[GenericRecord](schema)
    val dataFileWriter = new DataFileWriter[GenericRecord](datumWriter)

    val append = file.exists()
    if (append) dataFileWriter.appendTo(file)
    else dataFileWriter.create(schema, file)
    records.foreach(dataFileWriter.append)
    dataFileWriter.close()
  }

  def read(filename: String, schema: Schema) = {
    // Deserialize data from disk
    val file = new File(filename)
    val datumReader = new GenericDatumReader[GenericRecord](schema)
    val dataFileReader = new DataFileReader(file, datumReader)
    val records = scala.collection.mutable.ArrayBuffer[GenericData.Record]()
    while (dataFileReader.hasNext()) {
      records.append(dataFileReader.next().asInstanceOf[GenericData.Record])
    }
    dataFileReader.close()
    records.toSeq
  }
}
