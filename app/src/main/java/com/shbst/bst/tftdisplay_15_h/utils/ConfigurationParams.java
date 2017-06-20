package com.shbst.bst.tftdisplay_15_h.utils;

import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.google.gson.Gson;

import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import de.greenrobot.event.EventBus;

/**
 * Created by hegang on 2017-05-19.
 */
public class ConfigurationParams {
    private static final String TAG = "ConfigurationParams";
    private Context mContext;
    DocumentBuilderFactory factory;
    public static ConfigurationParams configurationParams = null;
    private String CFG_ROOT = "root";   //root 根节点
    private String CFG_RESET = "reset";  // 一键恢复默认
    private String CFG_RESOURCE = "resource";  // 资源配置节点
    private String CFG_PARAMETER = "parameter";  //参数配置节点

    private final String CFG_RESOURCE_TITLE = "title";
    private final String CFG_RESOURCE_SCROLLTEXT = "scrollingtext";
    private final String CFG_RESOURCE_PICTURE = "picture";
    private final String CFG_RESOURCE_VIDEO = "video";

    private final String CFG_PARAMETER_VOLUME = "volume";
    private final String CFG_PARAMETER_BRIGHTNESS = "brightness";
    private final String CFG_PARAMETER_STANDBY = "standby";
    private final String CFG_PARAMETER_FULLSCREEN = "fullscreen";
    private final String CFG_PARAMETER_SCROLLAREA = "scrollingarea";
    private final String CFG_PARAMETER_TITLEAREA = "titlearea";
    private final String CFG_PARAMETER_TIMERAREA = "timearea";

    private final String CFG_PARAMETER_STANDBY_STAGEONE = "stageone";
    private final String CFG_PARAMETER_STANDBY_STAGETWO = "stagetwo";

    private final String CFG_PARAMETER_STANDBY_TIME = "time";
    private final String CFG_PARAMETER_STANDBY_BRIGHTNESS = "brightness";

    private final String CFG_PARAMETER_FROMAT = "format";

    private final String CFG_PARAMETER_TIME_FROMAT = "time";
    private final String CFG_PARAMETER_DATE_FROMAT = "data";


    private final String UPDATA_RESOURCE = "updata/mulmedia/";

    String sourcePath = "";

    private EventBus parseEventBus = EventBus.getDefault();
    private boolean resetMediaScreen = false;   //是否一键恢复

    public static ConfigurationParams getConfigurationParams(Context context) {
        if (configurationParams == null) {
            configurationParams = new ConfigurationParams(context);
        }
        return configurationParams;
    }

    public ConfigurationParams(Context context) {
        this.mContext = context;
    }

    public void parseXML(InputStream is) {
        Message msg = new Message();
        msg.obj = is;
        parseXMLHandler.sendMessage(msg);
    }

    Handler parseXMLHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            InputStream data = (InputStream) msg.obj;
            // 创建DOM工厂对象
            try {
                MyLOG("parseXML 创建DOM工厂对象");
                factory = DocumentBuilderFactory.newInstance();
                // DocumentBuilder对象
                DocumentBuilder builder = null;
                MyLOG("parseXML DocumentBuilder对象");
                builder = factory.newDocumentBuilder();

                // 获取文档对象
                Document document = builder.parse(data);
                MyLOG("parseXML 获取文档对象");
                // 获取文档对象的root
                Element root = document.getDocumentElement();
                MyLOG("parseXML 获取文档对象的root");
                // 获取root根节点中所有的节点对象
                NodeList personNodes = root.getChildNodes();

                MyLOG("parseXML 获取SubUI根节点中所有的节点对象");
                for (int i = 0; i < personNodes.getLength(); i++) {
                    //  根据item(index)获取该索引对应的节点对象
                    //  Log.i(TAG, "parseXML: " + personNodes.item(i).getNodeName());
                    if (personNodes.item(i).getNodeName().equals(CFG_RESET)) {
                        String content = personNodes.item(i).getTextContent();
                        if (content.equals("true")) {
                            resetMediaScreen = true;
                        } else {
                            resetMediaScreen = false;
                        }
                        parseEventBus.post(new Params(CFG_RESET, String.valueOf(resetMediaScreen)));
                    }
                    if (!resetMediaScreen) {
                        if (personNodes.item(i).getNodeName().equals(CFG_RESOURCE)) {
                            Node n = personNodes.item(i);
                            NodeList nodelist = n.getChildNodes();
                            for (int j = 0; j < nodelist.getLength(); j++) {
                                if (nodelist.item(j) instanceof Element) {
//                                Log.i(TAG, "parseXML:CFG_RESOURCE " + nodelist.item(j).getNodeName());
                                    switch (nodelist.item(j).getNodeName()) {
                                        case CFG_RESOURCE_TITLE:
                                            String title = nodelist.item(j).getTextContent();
                                            parseEventBus.post(new Params(CFG_RESOURCE_TITLE, title));
                                            break;
                                        case CFG_RESOURCE_SCROLLTEXT:
                                            String scrolltext = nodelist.item(j).getTextContent();
                                            parseEventBus.post(new Params(CFG_RESOURCE_SCROLLTEXT, scrolltext));
                                            break;
                                        case CFG_RESOURCE_PICTURE:
                                            Element item = (Element) nodelist.item(j);
                                            String interval = item.getAttribute("interval");
                                            parseEventBus.post(new Params("interval", interval));
                                            NodeList mutimedia = nodelist.item(j).getChildNodes();
                                            for (int k = 0; k < mutimedia.getLength(); k++) {
                                                if (mutimedia.item(k) instanceof Element) {
                                                    parseEventBus.post(new Params(CFG_RESOURCE_PICTURE, mutimedia.item(k).getTextContent()));
                                                }
                                            }
                                            break;
                                        case CFG_RESOURCE_VIDEO:
                                            String video = nodelist.item(j).getTextContent();
                                            parseEventBus.post(new Params(CFG_RESOURCE_VIDEO, video));
                                            break;
                                    }
                                }
                            }
                        }
                        if (personNodes.item(i).getNodeName().equals(CFG_PARAMETER)) {
                            Node n = personNodes.item(i);
                            NodeList nodelist = n.getChildNodes();
                            for (int j = 0; j < nodelist.getLength(); j++) {
                                if (nodelist.item(j) instanceof Element) {
                                    switch (nodelist.item(j).getNodeName()) {
                                        case CFG_PARAMETER_VOLUME:
                                            String volume = nodelist.item(j).getTextContent();
                                            parseEventBus.post(new Params(CFG_PARAMETER_VOLUME, volume));
                                            break;
                                        case CFG_PARAMETER_BRIGHTNESS:
                                            String brightness = nodelist.item(j).getTextContent();
                                            parseEventBus.post(new Params(CFG_PARAMETER_BRIGHTNESS, brightness));
                                            break;
                                        case CFG_PARAMETER_FULLSCREEN:
                                            String fullscreen = nodelist.item(j).getTextContent();
                                            if(fullscreen.equals("true")){
                                                parseEventBus.post(new Params(CFG_PARAMETER_FULLSCREEN, "0"));
                                            }else{
                                                parseEventBus.post(new Params(CFG_PARAMETER_FULLSCREEN, "1"));
                                            }
                                            break;
                                        case CFG_PARAMETER_SCROLLAREA:
                                            String scrollingarea = nodelist.item(j).getTextContent();
                                            parseEventBus.post(new Params(CFG_PARAMETER_SCROLLAREA, scrollingarea));
                                            break;
                                        case CFG_PARAMETER_TITLEAREA:
                                            String titlearea = nodelist.item(j).getTextContent();
                                            parseEventBus.post(new Params(CFG_PARAMETER_TITLEAREA, titlearea));
                                            break;
                                        case CFG_PARAMETER_TIMERAREA:
                                            String timearea = nodelist.item(j).getTextContent();
                                            parseEventBus.post(new Params(CFG_PARAMETER_TIMERAREA, timearea));
                                            break;
                                        case CFG_PARAMETER_STANDBY:
                                            Node node_standby = nodelist.item(j);
                                            NodeList nodelist_standby = node_standby.getChildNodes();
                                            for (int k = 0; k < nodelist_standby.getLength(); k++) {
                                                if (nodelist_standby.item(k) instanceof Element) {
                                                    switch (nodelist_standby.item(k).getNodeName()){
                                                        case CFG_PARAMETER_STANDBY_STAGEONE:
                                                            Node node_standby_one = nodelist_standby.item(k);
                                                            NodeList nodelist_standby_one = node_standby_one.getChildNodes();
                                                            paramterStandby(CFG_PARAMETER_STANDBY_STAGEONE,nodelist_standby_one);
                                                            break;
                                                        case CFG_PARAMETER_STANDBY_STAGETWO:
                                                            Node node_standby_two = nodelist_standby.item(k);
                                                            NodeList nodelist_standby_two = node_standby_two.getChildNodes();
                                                            paramterStandby(CFG_PARAMETER_STANDBY_STAGETWO,nodelist_standby_two);
                                                            break;
                                                    }
                                                }
                                            }
                                            break;
                                        case CFG_PARAMETER_TIME_FROMAT:
                                            Node node_fromat = nodelist.item(j);
                                            NodeList nodelist_fromat = node_fromat.getChildNodes();
                                            for (int b = 0; b < nodelist_fromat.getLength(); b++) {
                                                if (nodelist_fromat.item(b) instanceof Element) {
                                                    String nodeName = nodelist_fromat.item(b).getTextContent();
                                                    parseEventBus.post(new Params(CFG_PARAMETER_TIME_FROMAT,nodeName));
                                                }
                                            }
                                            break;
                                        case CFG_PARAMETER_DATE_FROMAT:
                                            Node node_date_fromat = nodelist.item(j);
                                            NodeList nodelist_date_fromat = node_date_fromat.getChildNodes();
                                            for (int b = 0; b < nodelist_date_fromat.getLength(); b++) {
                                                if (nodelist_date_fromat.item(b) instanceof Element) {
                                                    String nodeName = nodelist_date_fromat.item(b).getTextContent();
                                                    parseEventBus.post(new Params(CFG_PARAMETER_DATE_FROMAT,nodeName));
                                                }
                                            }
                                            break;
                                    }
                                }
                            }
                        }
                    }
                }
            } catch (SAXException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (ParserConfigurationException e) {
                e.printStackTrace();
            }
        }
    };
    private void paramterStandby(String nodeName,NodeList nodelist){
        for (int a = 0; a < nodelist.getLength(); a++) {
            if (nodelist.item(a) instanceof Element) {
                switch (nodelist.item(a).getNodeName()){
                    case CFG_PARAMETER_STANDBY_TIME:
                        JSONObject json = new JSONObject();
                        try {
                            json.put(CFG_PARAMETER_STANDBY_TIME,nodelist.item(a).getTextContent());
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                        parseEventBus.post(new Params(nodeName, json.toString()));
                        MyLOG("node_standby_one  "+nodeName+"  :time "+nodelist.item(a).getTextContent());
                        break;
                    case CFG_PARAMETER_STANDBY_BRIGHTNESS:
                        JSONObject jsonb = new JSONObject();
                        try {
                            jsonb.put(CFG_PARAMETER_STANDBY_BRIGHTNESS,nodelist.item(a).getTextContent());
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                        parseEventBus.post(new Params(nodeName, jsonb.toString()));
                        MyLOG("node_standby_one  "+nodeName+"  :brightness "+nodelist.item(a).getTextContent());
                        break;
                }
            }
        }
    }

    private void MyLOG(String data) {
        Log.i(TAG, "MyLOG: " + data);
    }

    /**
     * 参数信息
     */
    public class Params {
        Gson gson = new Gson();
        public String type;   //配置类型
        public String info;   //配置信息

        public Params(String type, String info) {
            this.type = type;
            this.info = info;
        }

        @Override
        public String toString() {
            return "Params{" +
                    "type='" + type + '\'' +
                    ", info='" + info + '\'' +
                    '}';
        }
    }

    /**
     * 去除 路径前 包含的 file:// 字符串
     *
     * @param str
     * @param clearStr
     * @return
     */
    private static String clearStr(String str, String clearStr) {
        byte[] bytes = str.getBytes();
        byte[] clearStrBytes = clearStr.getBytes();
        byte[] StrTitleBytes = new byte[clearStrBytes.length];
        System.arraycopy(bytes, 0, StrTitleBytes, 0, StrTitleBytes.length);
        byte[] newByteTmp = null;
        if (Arrays.equals(clearStrBytes, StrTitleBytes)) {
            newByteTmp = new byte[bytes.length - clearStrBytes.length];
            System.arraycopy(bytes, clearStrBytes.length, newByteTmp, 0, newByteTmp.length);
        }
        if (newByteTmp != null) {
            return new String(newByteTmp);
        } else {
            return str;
        }
    }

}
