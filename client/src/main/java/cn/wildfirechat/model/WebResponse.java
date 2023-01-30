package cn.wildfirechat.model;

public class WebResponse<T> {
    public int code;
    public String message;
    public T result;

    public WebResponse() {

    }

    public WebResponse(int code, String msg) {
        this.code = code;
        this.message = msg;
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public T getResult() {
        return result;
    }

    public void setResult(T result) {
        this.result = result;
    }

    public boolean isSuccess() {
        return code == 0;
    }

    public static <T> WebResponse<T> createSuccess() {
        return new WebResponse<T>(0, "Success");
    }
}
