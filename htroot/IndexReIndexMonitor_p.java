
/**
 * IndexReIndexMonitor_p Copyright 2013 by Michael Peter Christen First released
 * 29.04.2013 at http://yacy.net
 *
 * This library is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 2.1 of the License, or (at your option)
 * any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program in the file lgpl21.txt If not, see
 * <http://www.gnu.org/licenses/>.
 */
import net.yacy.cora.federate.solr.connector.SolrConnector;
import net.yacy.cora.protocol.RequestHeader;
import net.yacy.cora.sorting.OrderedScoreMap;
import net.yacy.cora.util.ConcurrentLog;
import net.yacy.kelondro.workflow.BusyThread;

import java.io.IOException;

import net.yacy.migration;
import net.yacy.crawler.RecrawlBusyThread;
import net.yacy.data.TransactionManager;
import net.yacy.search.Switchboard;
import net.yacy.search.index.ReindexSolrBusyThread;
import net.yacy.server.serverObjects;
import net.yacy.server.serverSwitch;

public class IndexReIndexMonitor_p {

    public static serverObjects respond(@SuppressWarnings("unused") final RequestHeader header, final serverObjects post, final serverSwitch env) {

        final Switchboard sb = (Switchboard) env;
        final serverObjects prop = new serverObjects();
        
        /* Acquire a transaction token for the next possible POST form submissions */
        final String nextTransactionToken = TransactionManager.getTransactionToken(header);
        prop.put(TransactionManager.TRANSACTION_TOKEN_PARAM, nextTransactionToken);

        prop.put("docsprocessed", "0");
        prop.put("currentselectquery","");
        BusyThread reidxbt = sb.getThread(ReindexSolrBusyThread.THREAD_NAME);
        if (reidxbt == null) {
            if (post != null && post.containsKey("reindexnow") && sb.index.fulltext().connectedLocalSolr()) {
            	/* Check the transaction is valid */
            	TransactionManager.checkPostTransaction(header, post);
            	
                migration.reindexToschema(sb);
                prop.put("querysize", "0");
                prop.put("infomessage","reindex job started");
                
                reidxbt = sb.getThread(ReindexSolrBusyThread.THREAD_NAME); //get new created job for following posts
            }             
        }
        
        if (reidxbt != null) {
            prop.put("reindexjobrunning", 1);
            prop.put("querysize", reidxbt.getJobCount());

            if (reidxbt instanceof ReindexSolrBusyThread) {
                prop.put("docsprocessed", ((ReindexSolrBusyThread) reidxbt).getProcessed());
                prop.put("currentselectquery","q="+((ReindexSolrBusyThread) reidxbt).getCurrentQuery());
                // prepare list of fields in queue
                final OrderedScoreMap<String> querylist = ((ReindexSolrBusyThread) reidxbt).getQueryList();
                if (querylist != null) {
                    int i = 0;
                    for (String oneqs : querylist) { // just use fieldname from query (fieldname:[* TO *])
                        prop.put("reindexjobrunning_fieldlist_"+i+"_fieldname", oneqs.substring(0, oneqs.indexOf(':')));
                        prop.put("reindexjobrunning_fieldlist_"+i+"_fieldscore", querylist.get(oneqs));
                        i++;
                    }
                    prop.put("reindexjobrunning_fieldlist", querylist.size());
                } else {
                    prop.put("reindexjobrunning_fieldlist", 0);
                }
            }
            
            if (post != null && post.containsKey("stopreindex")) {
            	/* Check the transaction is valid */
            	TransactionManager.checkPostTransaction(header, post);
            	
                sb.terminateThread(ReindexSolrBusyThread.THREAD_NAME, false);
                prop.put("infomessage", "reindex job stopped");
                prop.put("reindexjobrunning",0);
            } else {
                prop.put("infomessage", "reindex is running");
            }            
        } else {
            prop.put("reindexjobrunning", 0);
            if (sb.index.fulltext().connectedLocalSolr()) {
                prop.put("querysize", "is empty");
                prop.put("infomessage", "no reindex job running");
            } else {
                prop.put("querysize", "");
                prop.putHTML("infomessage", "! reindex works only with embedded Solr index !");
            }
        }

        // recrawl job handling
        BusyThread recrawlbt = sb.getThread(RecrawlBusyThread.THREAD_NAME);
        
    	String recrawlQuery = RecrawlBusyThread.DEFAULT_QUERY;
        boolean inclerrdoc = RecrawlBusyThread.DEFAULT_INCLUDE_FAILED;
        // to signal that a setting shall change the form provides a fixed parameter setup=recrawljob, if not present return status only
        if (post != null && "recrawljob".equals(post.get("setup"))) { // it's a command to recrawlThread
        	
        	/* Check the transaction is valid */
        	TransactionManager.checkPostTransaction(header, post);
        	
        	if(post.containsKey("recrawlquerytext")) {
        		recrawlQuery = post.get("recrawlquerytext");
        	}
        	
            if (post.containsKey("includefailedurls")) {
                inclerrdoc = post.getBoolean("includefailedurls");
            }

            if (recrawlbt == null || recrawlbt.shutdownInProgress()) {
                prop.put("recrawljobrunning_simulationResult", 0);
                if (post.containsKey("recrawlnow") && sb.index.fulltext().connectedLocalSolr()) {
					sb.deployThread(RecrawlBusyThread.THREAD_NAME, "ReCrawl", "recrawl existing documents", null,
							new RecrawlBusyThread(Switchboard.getSwitchboard(), recrawlQuery, inclerrdoc), 1000);
                    recrawlbt = sb.getThread(RecrawlBusyThread.THREAD_NAME);
                } else if(post.containsKey("simulateRecrawl") && sb.index.fulltext().connectedLocalSolr()) {
                    SolrConnector solrConnector = sb.index.fulltext().getDefaultConnector();
                    if (!solrConnector.isClosed()) {
                        try {
                            // query all or only httpstatus=200 depending on includefailed flag
                            final long count = solrConnector.getCountByQuery(RecrawlBusyThread.buildSelectionQuery(recrawlQuery, inclerrdoc));
                            prop.put("recrawljobrunning_simulationResult", 1);
                            prop.put("recrawljobrunning_simulationResult_docCount", count);
                        } catch (final IOException e) {
                        	prop.put("recrawljobrunning_simulationResult", 2);
                        	ConcurrentLog.logException(e);
                        }
                    } else {
                    	prop.put("recrawljobrunning_simulationResult", 3);
                    }
                } else if(post.containsKey("recrawlDefaults")) {
                	recrawlQuery = RecrawlBusyThread.DEFAULT_QUERY;
                    inclerrdoc = RecrawlBusyThread.DEFAULT_INCLUDE_FAILED;
                }
            } else {
                if (post.containsKey("stoprecrawl")) {
                    sb.terminateThread(RecrawlBusyThread.THREAD_NAME, false);
                    prop.put("recrawljobrunning", 0);
                }
            }

            if (recrawlbt != null && !recrawlbt.shutdownInProgress()) {
                if (post.containsKey("updquery") && post.containsKey("recrawlquerytext")) {
                    ((RecrawlBusyThread) recrawlbt).setQuery(recrawlQuery, inclerrdoc);
                } else {
                    ((RecrawlBusyThread) recrawlbt).setIncludeFailed(inclerrdoc);
                }
            }
        }
        // just post status of recrawlThread
        if (recrawlbt != null && !recrawlbt.shutdownInProgress()) { // provide status
            prop.put("recrawljobrunning", 1);
            prop.put("recrawljobrunning_docCount", ((RecrawlBusyThread) recrawlbt).getUrlsToRecrawl());
            prop.put("recrawljobrunning_recrawlquerytext", ((RecrawlBusyThread) recrawlbt).getQuery());
            prop.put("recrawljobrunning_includefailedurls", ((RecrawlBusyThread) recrawlbt).getIncludeFailed());
        } else {
            prop.put("recrawljobrunning", 0);
            prop.put("recrawljobrunning_recrawlquerytext", recrawlQuery);
            prop.put("recrawljobrunning_includefailedurls", inclerrdoc);
        }

        // return rewrite properties
        return prop;
    }
}