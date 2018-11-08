package com.demandbase.spark.bigquery.converters

import org.apache.spark.sql.DataFrame
import org.apache.spark.sql.catalyst.encoders.RowEncoder
import org.apache.spark.sql.functions.current_timestamp
import org.apache.spark.sql.types._

/**
  * Make DataFrame's schema valid for importing to BigQuery.
  * The rules are:
  *
  * 1) Valid characters are: letters, digits and underscores
  * 2) All letters must be lowercase
  */
object BigQueryAdapter {

  private def adaptName(name: String, siblings: Array[String]): String = {
    var newName = name.replaceAll("\\W", "_")
    if (!newName.equals(name)) {
      // Avoid duplicates:
      var counter = 1;
      while (!siblings.find(_.equals(newName)).isEmpty) {
        newName = newName + "_" + counter
        counter = counter + 1
      }
    }
    newName
  }

  private def adaptField(structField: StructField, parentType: StructType): StructField = {
    new StructField(adaptName(structField.name, parentType.fieldNames), adaptType(structField.dataType), structField.nullable)
  }

  private def adaptType(dataType: DataType): DataType = {
    dataType match {
      case structType: StructType =>
        new StructType(structType.fields.map(adaptField(_, structType)))
      case arrayType: ArrayType =>
        new ArrayType(adaptType(arrayType.elementType), arrayType.containsNull)
      case mapType: MapType =>
        new MapType(adaptType(mapType.keyType), adaptType(mapType.valueType), mapType.valueContainsNull)
      case other => other
    }
  }

  def apply(df: DataFrame): DataFrame = {
    val sqlContext = df.sparkSession.sqlContext
    val sparkContext = df.sparkSession.sparkContext
    val timestampColumn = sparkContext
      .hadoopConfiguration.get("timestamp_column","bq_load_timestamp")
    val newSchema = adaptType(df.schema).asInstanceOf[StructType]
    val encoder = RowEncoder.apply(newSchema).resolveAndBind()
    val encodedDF = df
      .queryExecution
      .toRdd.map(x=>encoder.fromRow(x))
   sqlContext.createDataFrame(encodedDF,newSchema).withColumn(timestampColumn,current_timestamp())
  }
}
