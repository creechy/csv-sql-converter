# csv-sql-converter

Quickly made, bare bones tool to convert CSV files into SQL INSERT commands.

```
java CsvSqlConverter --csv input.csv --table table --output output.sql --types type,type,... [--tab] [--headers header,header...]
```

Where

| input.csv | name of the CSV file to load |
| table | name of the SQL table to INSERT into |
| output.sql | name of the output SQL file |
| --tab | switch to tab delimited instead of commas |
| type | datatypes of columns - string,int,integer,bool,boolean,float,number |
| header | optional column names - if csv does not include them |
