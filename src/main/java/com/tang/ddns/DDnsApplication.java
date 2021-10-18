package com.tang.ddns;

import com.aliyuncs.DefaultAcsClient;
import com.aliyuncs.IAcsClient;
import com.aliyuncs.alidns.model.v20150109.DescribeDomainRecordsRequest;
import com.aliyuncs.alidns.model.v20150109.DescribeDomainRecordsResponse;
import com.aliyuncs.alidns.model.v20150109.UpdateDomainRecordRequest;
import com.aliyuncs.exceptions.ClientException;
import com.aliyuncs.profile.DefaultProfile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author : tangjian
 * @date : 2021-08-31 14:21
 */
public class DDnsApplication {

    private static String ACCESS_KEY;
    private static String ACCESS_SECRET;
    private static String DOMAIN_NAME;
    private static String RR_KEYWORD;
    private static final String RECORD_TYPE = "a";

    private static IAcsClient CLIENT;

    private static Integer INTERVAL;

    private static final SimpleDateFormat SIMPLE_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");


    public static void main(String[] args) throws InterruptedException {


        initConfig();
        if (ACCESS_KEY == null || ACCESS_SECRET == null || DOMAIN_NAME == null) {
            log("参数配置错误");
            return;
        }
        DefaultProfile profile = DefaultProfile.getProfile("cn-hangzhou", ACCESS_KEY, ACCESS_SECRET);
        CLIENT = new DefaultAcsClient(profile);
        //noinspection InfiniteLoopStatement
        while (true) {
            execute();
            //noinspection BusyWait
            Thread.sleep(INTERVAL * 1000);
        }
    }


    public static void initConfig() {

        ACCESS_KEY = System.getenv("akey");
        ACCESS_SECRET = System.getenv("secret");
        DOMAIN_NAME = System.getenv("domain");
        RR_KEYWORD = System.getenv("rr");
        String interval = System.getenv("interval");
        INTERVAL = Integer.valueOf(interval == null ? "300" : interval);
        log("accessKey:" + ACCESS_SECRET);
        log("secret:" + ACCESS_SECRET);
        log("domain:" + DOMAIN_NAME);
        log("rr:" + RR_KEYWORD);
        log("type:" + RECORD_TYPE);
        log("interval:" + INTERVAL);

    }

    public static void log(String message) {
        System.out.printf("%s : %s%n", SIMPLE_DATE_FORMAT.format(new Date()), message);
    }


    public static void execute() {
        try {
            String newIp = getNewIp();
            log("获取到新IP：" + newIp);
            if (newIp == null || newIp.length() == 0) {
                return;
            }

            DescribeDomainRecordsResponse existRecord = getExistRecord();
            List<DescribeDomainRecordsResponse.Record> domainRecords = existRecord.getDomainRecords();
            for (DescribeDomainRecordsResponse.Record record : domainRecords) {
                String value = record.getValue();
                if (!newIp.equals(value)) {
                    update(newIp, record);
                }
            }
        } catch (Exception e) {
            log("执行异常");
            e.printStackTrace();
        }

    }

    public static void update(String newIp, DescribeDomainRecordsResponse.Record record) throws ClientException {
        UpdateDomainRecordRequest request = new UpdateDomainRecordRequest();
        request.setActionName("UpdateDomainRecord");
        request.setRecordId(record.getRecordId());
        request.setRR(record.getRR());
        request.setValue(newIp);
        request.setType(RECORD_TYPE);
        CLIENT.getAcsResponse(request);
        log(String.format("更新%s.%s的ip为%s", record.getRR(), record.getDomainName(), newIp));
    }


    public static String getNewIp() {
        BufferedReader in = null;
        try {
            String ipUrl = "https://ip.tool.lu/";
            StringBuilder inputLine = new StringBuilder();
            String read;
            URL url = new URL(ipUrl);
            HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
            in = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
            while ((read = in.readLine()) != null) {
                inputLine.append(read);
            }
            //  正则表达式，提取xxx.xxx.xxx.xxx，将IP地址从接口返回结果中提取出来
            String rexp = "(\\d{1,3}\\.){3}\\d{1,3}";
            Pattern pat = Pattern.compile(rexp);
            Matcher mat = pat.matcher(inputLine);
            String res = "";
            //noinspection LoopStatementThatDoesntLoop
            while (mat.find()) {
                res = mat.group();
                break;
            }
            return res;
        } catch (Exception e) {
            e.printStackTrace();
            return "";
        } finally {
            try {
                if (in != null) {
                    in.close();
                }
            } catch (Exception e2) {
                e2.printStackTrace();
            }
        }
    }


    public static DescribeDomainRecordsResponse getExistRecord() throws ClientException {
        DescribeDomainRecordsRequest request = new DescribeDomainRecordsRequest();
        request.setRRKeyWord(RR_KEYWORD);
        request.setDomainName(DOMAIN_NAME);
        request.setActionName("DescribeDomainRecords");
        request.setTypeKeyWord(RECORD_TYPE);
        return CLIENT.getAcsResponse(request);
    }
}
