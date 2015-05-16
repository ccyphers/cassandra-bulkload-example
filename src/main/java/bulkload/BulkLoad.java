/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package bulkload;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.List;

import org.supercsv.io.CsvListReader;
import org.supercsv.prefs.CsvPreference;

import org.apache.cassandra.config.Config;
import org.apache.cassandra.dht.Murmur3Partitioner;
import org.apache.cassandra.exceptions.InvalidRequestException;
import org.apache.cassandra.io.sstable.CQLSSTableWriter;

/**
 * Usage: java bulkload.BulkLoad
 */
public class BulkLoad {
	// public static final String CSV_URL =
	// "http://real-chart.finance.yahoo.com/table.csv?s=%s";

	/** Default output directory */
	public static final String DEFAULT_OUTPUT_DIR = "/tmp/data";
	public static final String TICKER = "goog";
	public static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat(
			"yyyy-MM-dd");

	/** Keyspace name */
	public static final String KEYSPACE = "quote";
	/** Table name */
	public static final String TABLE = "historical_prices";

	/**
	 * Schema for bulk loading table. It is important not to forget adding
	 * keyspace name before table name, otherwise CQLSSTableWriter throws
	 * exception.
	 */
	public static final String SCHEMA = String.format("CREATE TABLE %s.%s ("
			+ "ticker ascii, " + "date timestamp, " + "open decimal, "
			+ "high decimal, " + "low decimal, " + "close decimal, "
			+ "volume bigint, " + "adj_close decimal, "
			+ "PRIMARY KEY (ticker, date) "
			+ ") WITH CLUSTERING ORDER BY (date DESC)", KEYSPACE, TABLE);

	/**
	 * INSERT statement to bulk load. It is like prepared statement. You fill in
	 * place holder for each data.
	 */
	public static final String INSERT_STMT = String.format(
			"INSERT INTO %s.%s ("
					+ "ticker, date, open, high, low, close, volume, adj_close"
					+ ") VALUES (" + "?, ?, ?, ?, ?, ?, ?, ?" + ")", KEYSPACE,
			TABLE);

	public static void main(String[] args) {
		Config.setClientMode(true);

		// Create output directory that has keyspace and table name in the path
		File outputDir = new File(DEFAULT_OUTPUT_DIR + File.separator
				+ KEYSPACE + File.separator + TABLE);
		if (!outputDir.exists() && !outputDir.mkdirs()) {
			throw new RuntimeException("Cannot create output directory: "
					+ outputDir);
		}

		CQLSSTableWriter writer = CQLSSTableWriter.builder()
                .inDirectory(outputDir)
                .forTable(SCHEMA)
                .using(INSERT_STMT).build();
		
		
        try (
                BufferedReader reader = new BufferedReader(new FileReader("/tmp/tmp.csv"));
                CsvListReader csvReader = new CsvListReader(reader, CsvPreference.STANDARD_PREFERENCE)
            )
            {
                

                csvReader.getHeader(true);

                // Write to SSTable while reading data
                List<String> line;
                while ((line = csvReader.read()) != null)
                {
                	if(line.size() == 7) {
                		System.out.println(line.get(0));
                    // We use Java types here based on
                    // http://www.datastax.com/drivers/java/2.0/com/datastax/driver/core/DataType.Name.html#asJavaClass%28%29
                    writer.addRow(TICKER,
                                  DATE_FORMAT.parse(line.get(0)),
                                  new BigDecimal(line.get(1)),
                                  new BigDecimal(line.get(2)),
                                  new BigDecimal(line.get(3)),
                                  new BigDecimal(line.get(4)),
                                  Long.parseLong(line.get(5)),
                                  new BigDecimal(line.get(6)));
                	}
                }
            }
            catch (InvalidRequestException | ParseException | IOException e)
            {
                e.printStackTrace();
            }	
        
        try
        {
            writer.close();
        }
        catch (IOException ignore) {}
	}
}
