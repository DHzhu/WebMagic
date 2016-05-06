package us.codecraft.webmagic.example;

import com.google.common.collect.ImmutableList;
import us.codecraft.webmagic.Site;
import us.codecraft.webmagic.Task;
import us.codecraft.webmagic.model.OOSpider;
import us.codecraft.webmagic.model.annotation.ExtractBy;
import us.codecraft.webmagic.model.annotation.Formatter;
import us.codecraft.webmagic.model.annotation.TargetUrl;
import us.codecraft.webmagic.pipeline.PageModelPipeline;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author yihua.huang@dianping.com
 * @date 14-3-28
 */
@TargetUrl("http://w.51ping.com/search/category/1/10/*")
public class DianpingSearch {

	@Formatter(subClazz = Integer.class)
	@ExtractBy("//li[@class=shopname]/regex(\"shop.{1}(\\d+)\",1)")
	private List<Integer> shopIds;

	@ExtractBy("//li[@class=shopname]")
	private List<String> classNames;

	public static void main(String[] args) {
        PageModelPipeline<DianpingSearch> pageModelPipeline = new PageModelPipeline<DianpingSearch>() {
            @Override
            public void process(DianpingSearch o, Task task) {
                for (Integer shopId : o.shopIds) {
                    System.out.println(String.format(
                            "INSERT INTO `MOPay_Shop` ( `ShopID`, `Status`, `AddTime`, `UpdateTime`)\n" + "VALUES\n"
                                    + "\t(%d, 1, '2014-03-27 15:54:46', '2014-03-27 15:54:46');\n", shopId));
                }
            }
        };
        OOSpider ooSpider = OOSpider.create(Site.me().setSleepTime(0), pageModelPipeline, DianpingSearch.class);
        ooSpider.addUrl("http://w.51ping.com/search/category/1/10/g198r842p2",
                "http://w.51ping.com/search/category/1/10/g198r842p3",
                "http://w.51ping.com/search/category/1/10/g210r842",
                "http://w.51ping.com/search/category/1/10/g112r842").thread(10).run();

	}

    private void s(){
        OOSpider ooSpider = OOSpider.create(Site.me().setSleepTime(0), DianpingSearch.class);
        // single download
        DianpingSearch dianpingSearch = ooSpider
                .<DianpingSearch> get("http://w.51ping.com/search/category/1/10/g198r842");
        List<DianpingSearch> all = ooSpider.<DianpingSearch>getAll(ImmutableList.of("http://w.51ping.com/search/category/1/10/g198r842",
                "http://w.51ping.com/search/category/1/10/g198r842p2",
                "http://w.51ping.com/search/category/1/10/g198r842p3",
                "http://w.51ping.com/search/category/1/10/g210r842",
                "http://w.51ping.com/search/category/1/10/g112r842"));

        Set<Integer> ids = new HashSet<Integer>();
        for (DianpingSearch search : all) {
            for (Integer shopId : search.shopIds) {
                ids.add(shopId);
            }
        }

        for (Integer shopId : ids) {
            System.out.println(String.format(
                    "INSERT INTO `MOPay_Shop` ( `ShopID`, `Status`, `AddTime`, `UpdateTime`)\n" + "VALUES\n"
                            + "\t(%d, 1, '2014-03-27 15:54:46', '2014-03-27 15:54:46');\n", shopId));
        }
    }
}
