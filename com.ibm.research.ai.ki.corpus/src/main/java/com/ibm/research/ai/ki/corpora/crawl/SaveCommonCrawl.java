/**
 * cc-dbp-dataset
 *
 * Copyright (c) 2017 IBM
 *
 * The author licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.ibm.research.ai.ki.corpora.crawl;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.zip.*;

import com.ibm.reseach.ai.ki.nlp.*;
import com.ibm.reseach.ai.ki.nlp.types.*;
import com.ibm.research.ai.ki.util.*;
import com.ibm.research.ai.ki.util.FileUtil;

import org.apache.commons.cli.*;
import org.apache.commons.cli.Options;
import org.apache.commons.io.*;
import org.apache.hadoop.conf.*;
import org.apache.hadoop.fs.*;
import org.apache.hadoop.fs.FileSystem;
import org.jwat.common.*;
import org.jwat.warc.*;


/**
 * See the common-crawl blog for updates and new crawls: http://commoncrawl.org/connect/blog/
 * For example:
 * http://commoncrawl.org/2017/07/july-2017-crawl-archive-now-available/
 *
 * Saves the English non-boilerplate text from webpages into normal (non-HDFS) filesystem.
 * @author mrglass
 *
 */
public class SaveCommonCrawl extends SaveCommonCrawlBase {
    
    public SaveCommonCrawl(CommonCrawlConfig config) {
        super(config);
    }


    @Override
    protected PrintStream getThreadOutput(String baseDir, int threadNum) {
        //CONSIDER: support resuming by making sure to not overwrite a file (creating a new file instead)
        return FileUtil.getFilePrintStream(new File(baseDir, "part-"+threadNum).getAbsolutePath());
    }

    
    /**
     * Example args:
       -urlList /somewhere/june2017-warc.paths -out /somewhere/june2017-docs.json.gz.b64 -config cc-dbp/cc-dbp.properties
     * @param args
     * @throws Exception
     */
    public static void main(String[] args) throws Exception {
        Options options = new Options();
        options.addOption("config", true, "A RelexConfig in properties file format");   
        options.addOption("urlList", true, "List of WARC urls, see http://commoncrawl.org/connect/blog/");
        options.addOption("out", true, "The directory to create the dataset in");
        CommandLineParser parser = new DefaultParser();
        CommandLine cmd = null;
        try {
            cmd = parser.parse(options, args);  
        } catch (ParseException pe) {
            Lang.error(pe);
        }
        
        String configProperties = cmd.getOptionValue("config");
        String warcUrlList = cmd.getOptionValue("urlList");
        String targetDir = cmd.getOptionValue("out");
        
        if (new File(targetDir).exists() && new File(targetDir).list().length > 0) {
            System.out.println("Skipping Common Crawl download, using existing "+targetDir);
            return;
        }
        
        CommonCrawlConfig config = new CommonCrawlConfig();
        config.fromProperties(PropertyLoader.loadProperties(configProperties));

        SaveCommonCrawl scc = new SaveCommonCrawl(config);
        scc.warcDir = targetDir+"-logs";
        scc.saveFormat = DocumentSerialize.formatFromName(targetDir);
        Iterable<String> warcUrls = FileUtil.getLines(warcUrlList);
        
        //TODO: check if any of the warcUrls are in the finished list, if so, exclude them.
        //  if we resume from a failure, we will still need to check for duplicates within a warc file
        
        scc.writeCrawl(warcUrls, targetDir);
    }
}
