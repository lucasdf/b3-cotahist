# b3-cotahist

Parser for historical quotes files from B3.

## Usage

Using the sample file to query prices:

```
$ lein uberjar
$ java -jar target/b3-cotahist.jar -f resources/DemoCotacoesHistoricas12022003.txt -t UBBR4T -t VALE5 -d 2003-02-12| jq
[
  {
    "ticket": "UBBR4T",
    "price_close": 35.29,
    "date": "2003-02-12"
  },
  {
    "ticket": "VALE5T",
    "price_close": 101.94,
    "date": "2003-02-12"
  }
]
```

You can download the historical files using:
```
$ java -jar target/b3-cotahist.jar download
```

Generating [hledger](https://github.com/simonmichael/hledger) compatible price db file:
```
java -jar target/b3-cotahist.jar -f resources/DemoCotacoesHistoricas12022003.txt | jq -r '.[] | "P \(.date) \"\(.ticket)\" \(.price_close)"'
```

## Similars
- https://github.com/diogolr/b3parser

## License

Copyright © 2020 FIXME

This program and the accompanying materials are made available under the
terms of the Eclipse Public License 2.0 which is available at
http://www.eclipse.org/legal/epl-2.0.

This Source Code may also be made available under the following Secondary
Licenses when the conditions for such availability set forth in the Eclipse
Public License, v. 2.0 are satisfied: GNU General Public License as published by
the Free Software Foundation, either version 2 of the License, or (at your
option) any later version, with the GNU Classpath Exception which is available
at https://www.gnu.org/software/classpath/license.html.
