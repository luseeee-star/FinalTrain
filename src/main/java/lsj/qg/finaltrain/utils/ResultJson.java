package lsj.qg.finaltrain.utils;

public class ResultJson<T> {
    private Integer code;   // 状态码
    private String msg;     // 消息
    private T data;         // 数据载体

    public ResultJson() {
    }

    public ResultJson(Integer code, String msg, T data) {
        this.code = code;
        this.msg = msg;
        this.data = data;
    }

    // 成功：无数据
    public static <T> ResultJson<T> success() {
        return new ResultJson<>(200, "操作成功", null);
    }

    // 成功：有数据
    public static <T> ResultJson<T> success(T data) {
        return new ResultJson<>(200, "操作成功", data);
    }

    // 成功：自定义消息 + 数据
    public static <T> ResultJson<T> success(String msg, T data) {
        return new ResultJson<>(200, msg, data);
    }

    // 失败：系统错误
    public static <T> ResultJson<T> error(String msg) {
        return new ResultJson<>(500, msg, null);
    }

    // 失败：系统异常（固定文案，避免暴露后端细节）
    public static <T> ResultJson<T> systemError() {
        return new ResultJson<>(500, "系统繁忙，请稍后重试", null);
    }

    // 失败：自定义状态码 + 消息
    public static <T> ResultJson<T> error(Integer code, String msg) {
        return new ResultJson<>(code, msg, null);
    }

    /**
     * 获取
     * @return code
     */
    public Integer getCode() {
        return code;
    }

    /**
     * 设置
     * @param code
     */
    public void setCode(Integer code) {
        this.code = code;
    }

    /**
     * 获取
     * @return msg
     */
    public String getMsg() {
        return msg;
    }

    /**
     * 设置
     * @param msg
     */
    public void setMsg(String msg) {
        this.msg = msg;
    }

    /**
     * 获取
     * @return data
     */
    public T getData() {
        return data;
    }

    /**
     * 设置
     * @param data
     */
    public void setData(T data) {
        this.data = data;
    }

    public String toString() {
        return "ResultJson{code = " + code + ", msg = " + msg + ", data = " + data + "}";
    }
}
