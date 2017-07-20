/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.spark.sql.execution.benchmark

import java.io.{File, FilenameFilter}
import java.util.Properties

import org.apache.spark.SparkFunSuite
import org.apache.spark.sql.execution.benchmark.packages._
import org.apache.spark.sql.test.SharedSQLContext


class TpcdsDatagenSuite extends SparkFunSuite with SharedSQLContext {

  test("conf") {
    val origProps = System.getProperties
    try {
      val props = new Properties()
      props.setProperty("spark.sql.dsdgen.scaleFactor", "3")
      props.setProperty("spark.sql.dsdgen.format", "csv")
      props.setProperty("spark.sql.dsdgen.overwrite", "true")
      props.setProperty("spark.sql.dsdgen.partitionTables", "true")
      props.setProperty("spark.sql.dsdgen.useDoubleForDecimal", "true")
      props.setProperty("spark.sql.dsdgen.clusterByPartitionColumns", "true")
      props.setProperty("spark.sql.dsdgen.filterOutNullPartitionValues", "true")
      props.setProperty("spark.sql.dsdgen.tableFilter", "testTable")
      props.setProperty("spark.sql.dsdgen.numPartitions", "12")
      System.setProperties(props)

      val conf = TpcdsConf()
      assert(conf.getInt("scaleFactor", 1) === 3)
      assert(conf.get("format", "parquet") === "csv")
      assert(conf.getBoolean("overwrite", false) === true)
      assert(conf.getBoolean("partitionTables", false) === true)
      assert(conf.getBoolean("useDoubleForDecimal", false) === true)
      assert(conf.getBoolean("clusterByPartitionColumns", false) === true)
      assert(conf.getBoolean("filterOutNullPartitionValues", false) === true)
      assert(conf.get("tableFilter", "") === "testTable")
      assert(conf.getInt("numPartitions", 100) === 12)
    } finally {
      System.setProperties(origProps)
    }
  }

  ignore("datagen") {
    val outputTempDir = createTempDir()
    val tpcdsTables = new Tables(spark.sqlContext, 1)
    tpcdsTables.genData(
      location = outputTempDir.getAbsolutePath,
      format = "parquet",
      overwrite = false,
      partitionTables = false,
      useDoubleForDecimal = false,
      clusterByPartitionColumns = false,
      filterOutNullPartitionValues = false,
      tableFilter = "",
      numPartitions = 4)

    val tpcdsExpectedTables = Set(
      "call_center", "catalog_page", "catalog_returns", "catalog_sales", "customer",
      "customer_address", "customer_demographics", "date_dim", "household_demographics",
      "income_band", "inventory", "item", "promotion", "reason", "ship_mode", "store",
      "store_returns", "store_sales", "time_dim", "warehouse", "web_page", "web_returns",
      "web_sales", "web_site")

    assert(outputTempDir.list.toSet === tpcdsExpectedTables)

    // Checks if output test data generated in each table
    val filenameFilter = new FilenameFilter {
      override def accept(dir: File, name: String): Boolean = name.endsWith(".parquet")
    }
    tpcdsExpectedTables.foreach { table =>
      val f = new File(s"${outputTempDir.getAbsolutePath}/$table").listFiles(filenameFilter)
      assert(f.size === 1)
    }
  }
}
