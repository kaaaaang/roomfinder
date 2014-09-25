package roomfinder.domain;

/**
 * Created with IntelliJ IDEA.
 * User: akang
 * Date: 9/24/14
 * Time: 2:18 PM
 * To change this template use File | Settings | File Templates.
 */
public class Room {
    private String name;
    private String email;
    private int capacity;
    private boolean isCasual;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public int getCapacity() {
        return capacity;
    }

    public void setCapacity(int capacity) {
        this.capacity = capacity;
    }

    public boolean isCasual() {
        return isCasual;
    }

    public void setCasual(boolean casual) {
        isCasual = casual;
    }
}
