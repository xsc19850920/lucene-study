package test;

/**
 * $Id$
 * Copyright 2009-2010 Oak Pacific Interactive. All rights reserved.
 */

import index.IndexResult;
import index.IndexUtils;

import java.util.Date;
import java.util.List;

import org.apache.lucene.index.ExitableDirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.IndexSearcher;

public class Test {

    //存放索引文件
    private static String indexFile = "E:\\workspace2\\Test\\lucene_test\\poiIdext";

    //存放id
    private static String storeIdFile = "E:\\workspace2\\Test\\lucene_test\\storeId.txt";

    public static void main(String[] args) throws Exception {
    	
       /* //0. 创建增量索引
        IndexUtils.buildIndex(indexFile, storeIdFile);
        IndexReader indexReader = new ExitableDirectoryReader();
       // IndexSearcher indexSearcher = new IndexSearcher(indexFile);
        String key = IndexUtils.ik_CAnalyzer("静安中心");

        //1.单字段查询
        Date date1 = new Date();
        List<IndexResult> list = IndexUtils.queryByOneKey(indexSearcher, "name", key);
        Date date2 = new Date();
        System.out.println("耗时：" + (date2.getTime() - date1.getTime()) + "ms\n" + list.size()
                + "条=======================================单字段查询");
        //printResults(list);

        //2.多条件查询
        String[] fields = { "name", "citycode" };
        String[] keys = { IndexUtils.ik_CAnalyzer("静安中心"), "0000" };
        date1 = new Date();
        list = IndexUtils.queryByMultiKeys(indexSearcher, fields, keys);
        date2 = new Date();
        System.out.println("耗时：" + (date2.getTime() - date1.getTime()) + "ms\n" + list.size()
                + "条\n===============================多条件查询");
        printResults(list);

        //3.高亮显示  单字段查询
        System.out.println("\n\n");
        date1 = new Date();
        list = IndexUtils.highlight(indexSearcher, key);
        date2 = new Date();
        System.out.println("耗时：" + (date2.getTime() - date1.getTime()) + "ms\n" + list.size()
                + "条\n======================================高亮显示");
       // printResults(list);

        //4. 多字段查询
        date1 = new Date();
        list = IndexUtils.queryByMultiFileds(indexSearcher, fields, key);
        date2 = new Date();
        System.out.println("耗时：" + (date2.getTime() - date1.getTime()) + "ms\n" + list.size()
                + "条\n=====================================多字段查询");
       // printResults(list);

        //5. 删除索引中的字段  根据id进行删除
        IndexUtils.deleteIndex(indexFile, "123");*/
    }

    //打印结果
    public static void printResults(List<IndexResult> list) {
        if (list != null && list.size() > 0) {
            for (int i = 0; i < list.size(); i++) {
                System.out.println(list.get(i).getId() + "," + list.get(i).getName() + ","
                        + list.get(i).getAddress() + "," + list.get(i).getCitycode()+"--->"+i);
            }
        }
    }
}
