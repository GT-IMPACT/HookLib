public class hook {
    public static int evaluation(int a, int b) {
        return a * a * 111;
    }

    public int a=0;

    public int lol() {
        if (a == 0)
            return 1;
        else if (a == 1)
            return 2;
        else
            return 3;
    }
}