masterdoc-client-maven-plugin
=============================

This project generate automatic webclients (Entities and RestWebClient) to be used in continous testing environment .


Just insert this in your pom.xml :
```<plugin>
                   <groupId>fr.masterdocs</groupId>
                   <artifactId>masterdoc-client-maven-plugin</artifactId>
                   <version>1.0-SNAPSHOT</version>
                   <executions>
                       <execution>
                           <phase>compile</phase>
                           <goals>
                               <goal>masterdoc-client</goal>
                           </goals>
                       </execution>
                   </executions>
                   <configuration>
                       <masterdocsFilePath>
                           PATH_OF_THE_JSON_FILE/masterdocs.json
                       </masterdocsFilePath>
                   </configuration>
               </plugin>

```
