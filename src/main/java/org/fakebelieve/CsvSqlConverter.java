package org.fakebelieve;

import org.supercsv.cellprocessor.*;
import org.supercsv.cellprocessor.ift.CellProcessor;
import org.supercsv.io.CsvListReader;
import org.supercsv.io.ICsvListReader;
import org.supercsv.prefs.CsvPreference;

import java.io.*;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.StringTokenizer;

/**
 * Hello world!
 */
public class CsvSqlConverter {
    public static void main(String[] args) throws IOException {
        ICsvListReader listReader = null;


        String csvFilename = null;
        String typeList = null;
        String tableName = null;
        String outputFilename = null;
        String headerList = null;
        boolean tabDelimited = false;

        for (int idx = 0; idx < args.length; idx++) {
            if (args[idx].equals("--csv")) {
                csvFilename = args[++idx];
                continue;
            }
            if (args[idx].equals("--table")) {
                tableName = args[++idx];
                continue;
            }
            if (args[idx].equals("--output")) {
                outputFilename = args[++idx];
                continue;
            }
            if (args[idx].equals("--tab")) {
                tabDelimited = true;
                continue;
            }
            if (args[idx].equals("--types")) {
                typeList = args[++idx];
                continue;
            }
            if (args[idx].equals("--headers")) {
                headerList = args[++idx];
                continue;
            }
        }
        
        PrintWriter output = new PrintWriter(new BufferedWriter(new FileWriter(outputFilename)));
        
        try {
            CsvPreference tabPreference = new CsvPreference.Builder('\0', '\t', "\n").build();
            listReader = new CsvListReader(new FileReader(csvFilename), (tabDelimited) ? tabPreference:CsvPreference.STANDARD_PREFERENCE);

            final String[] headers = (headerList != null) ? getArray(headerList):listReader.getHeader(true);
            final String[] types = getArray(typeList);
            final CellProcessor[] processors = getProcessors(types);

            List<Integer> fieldLengths = new ArrayList<>();
            List<Object> fieldList;
            while ((fieldList = listReader.read(processors)) != null) {
//                System.err.println(String.format("lineNo=%s, rowNo=%s, fieldList=%s", listReader.getLineNumber(),
//                        listReader.getRowNumber(), fieldList));

                output.print("INSERT INTO " + tableName + " VALUES(");
                boolean firstColumn = true;
                for (int idx = 0; idx < fieldList.size(); idx++) {
                    Object item = fieldList.get(idx);
                    int fieldLength = 0;

                    if (!firstColumn) {
                        output.print(",");
                    }
                    if (item == null) {
                        output.print("null");
                    }
                    else if (item instanceof Number) {
                        output.print(item);
                        fieldLength = item.toString().length();
                    } else {
                        output.print("'" + item.toString().replace("'", "''") + "'");
                        fieldLength = item.toString().length();
                    }
                    firstColumn = false;

                    if (fieldLengths.size() <= idx) {
                        fieldLengths.add(fieldLength);
                    } else {
                        fieldLength = Math.max(fieldLength, fieldLengths.get(idx));
                        fieldLengths.set(idx, fieldLength);
                    }
                }
                output.println(");");
            }

            output.flush();
            output.close();

            System.out.format("CREATE TABLE %s (\n", tableName);

            for (int idx = 0; idx < types.length; idx++) {
                String type = types[idx];
                switch (type.toLowerCase()) {
                    case "int":
                    case "integer":
                        System.out.format("  %s INTEGER(%d)", headers[idx], fieldLengths.get(idx));
                        break;
                    case "bool":
                    case "boolean":
                        System.out.format("  %s TINYINT(1)", headers[idx]);
                        break;
                    case "string":
                        System.out.format("  %s VARCHAR(%d)", headers[idx], fieldLengths.get(idx));
                        break;
                    case "number":
                    case "float":
                        System.out.format("  %s DECIMAL(%s)", headers[idx], fieldLengths.get(idx));
                        break;
                    default:
                        System.out.format("  %s VARCHAR(%d)", headers[idx], fieldLengths.get(idx));
                }
                if (idx + 1 < types.length) {
                    System.out.println(",");
                } else {
                    System.out.println();
                }
            }

            System.out.format(") DEFAULT CHARSET=utf8;");
        } finally {
            if (listReader != null) {
                listReader.close();
            }
        }

    }

    private static String[] getArray(String arg) {
        List<String> types = new LinkedList<>();
        StringTokenizer st = new StringTokenizer(arg, ",");
        while (st.hasMoreTokens()) {
            types.add(st.nextToken());
        }

        return types.toArray(new String[types.size()]);
    }

    private static CellProcessor[] getProcessors(String[] types) {
        CellProcessor[] processors = new CellProcessor[types.length];
        for (int idx = 0; idx < types.length; idx++) {
            String type = types[idx];
            switch (type.toLowerCase()) {
                case "int":
                case "integer":
                    processors[idx] = new Optional(new ParseInt());
                    break;
                case "bool":
                case "boolean":
                    processors[idx] = new Optional(new ParseBool());
                    break;
                case "string":
                    processors[idx] = new Optional();
                    break;
                case "number":
                case "float":
                    processors[idx] = new Optional(new ParseBigDecimal());
                    break;
                default:
                    processors[idx] = new Optional();
            }
        }
        return processors;
    }
}
