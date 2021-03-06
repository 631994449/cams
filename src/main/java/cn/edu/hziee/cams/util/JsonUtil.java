package cn.edu.hziee.cams.util;

import com.alibaba.fastjson.JSONObject;

/**
 * Copied by HuangChao on 2021/3/25
 */
public class JsonUtil {


    private final static JSONObject jsonObject = new JSONObject();

    /**
     * @Author MRC
     * @Description //TODO 失败的返回类型
     * @Date 10:50 2019/8/3
     * @Param [msg]
     * @return com.alibaba.fastjson.JSONObject
     **/
    public static JSONObject getFailJson(String msg) {
        return result(msg,null,0);
    }

    public static JSONObject getFailJson(String msg,Object object) {
        return result(msg,object,0);
    }
    
    
    /**
     * @Author MRC
     * @Description //TODO 成功的返回类型
     * @Date 11:04 2019/8/3
     * @Param [msg]
     * @return com.alibaba.fastjson.JSONObject
     **/
    public static JSONObject getSuccessJson(String msg) {
        return result(msg,null,0);
    }

    public static JSONObject getSuccessJson(String msg,Object object) {
        return result(msg,object,0);
    }


    /**
     * @Author MRC
     * @Description //TODO 加入同步锁的返回类
     * @Date 10:45 2019/8/3
     * @Param []
     * @return com.alibaba.fastjson.JSONObject
     **/
    public static synchronized JSONObject result(String msg, Object obj, Integer code){
        jsonObject.clear();

        jsonObject.put("msg",msg);
        jsonObject.put("data",obj);
        jsonObject.put("code",code);

        return jsonObject;
    }
}