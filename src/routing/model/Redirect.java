package routing.model;

public class Redirect {

    private int status;
    private String to;

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public String getTo() {
        return to;
    }

    public void setTo(String to) {
        this.to = to;
    }

    @Override
    public String toString() {
        return "Redirect{status=" + status + ", to='" + to + "'}";
    }
}
