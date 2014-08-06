/*
 * Copyright (c) 2014 by Clavain Technologies GbR.
 * http://www.clavain.com
 */
package com.clavain.rca;

import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import static com.clavain.muninmxcd.logger;
import static com.clavain.muninmxcd.m;
import static com.clavain.muninmxcd.p;
/**
 *
 * @author enricokern
 */
public class Methods {

    private static DB db;
    private static DBCollection col;
    private static DBCursor cursor;

    public static BigDecimal getTotalForPluginAndGraph(String p_plugin, String p_graph, int start, int end, int userId, int nodeId) {
        BigDecimal total = new BigDecimal("0").setScale(2,RoundingMode.HALF_UP);
        try {
            String dbName = com.clavain.muninmxcd.p.getProperty("mongo.dbname");
            db = m.getDB(dbName);
            col = db.getCollection(userId + "_" + nodeId);


            BasicDBObject query = new BasicDBObject("plugin", p_plugin);
            query.append("graph", p_graph);

            BasicDBObject gtlt = new BasicDBObject("$gt", start);
            gtlt.append("$lt", end);
            query.append("recv", gtlt);

            cursor = col.find(query);


            try {
                while (cursor.hasNext()) {

                    BigDecimal val = new BigDecimal(cursor.next().get("value").toString());
                    val.setScale(2,RoundingMode.HALF_UP);
                    total = total.add(val);
                }
            } finally {
                cursor.close();
            }

        } catch (Exception ex) {
            logger.error("[RCA] Error in getTotalForPluginAndGraph: " + ex.getLocalizedMessage());
            ex.printStackTrace();
        }
        return total;
    }

    // A = average, b = total
    public static BigDecimal ReversePercentageFromValues(BigDecimal a, BigDecimal b) {
        try {
            a.setScale(2,RoundingMode.HALF_UP);
            b.setScale(2,RoundingMode.HALF_UP);
            a = a.multiply(new BigDecimal(100).setScale(2,RoundingMode.HALF_UP));
            
            a = a.divide(b, 2, RoundingMode.HALF_UP);
            a = new BigDecimal(100).setScale(2,RoundingMode.HALF_UP).subtract(a);
            return a;
        } catch (Exception ex)
        {
            ex.printStackTrace();
            logger.error("Error in ReversePercentageFromValues: a = " + a + " b = " + b);
            return new BigDecimal("0");
        }
    }

    public static BigDecimal returnAvgBig(ArrayList<BigDecimal> p_values) {
        BigDecimal numbers = new BigDecimal(p_values.size());
        BigDecimal retval = new BigDecimal(0).setScale(2,RoundingMode.HALF_UP);
        for (BigDecimal l_av : p_values) {
            l_av.setScale(2,RoundingMode.HALF_UP);
            // retval += l_av.doubleValue();
            retval = retval.add(l_av);
        }
        BigDecimal average = retval;
        average = average.divide(new BigDecimal(p_values.size()), 2, RoundingMode.HALF_UP);
        return average;
    }
}
