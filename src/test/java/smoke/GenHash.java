package smoke;

import org.mindrot.jbcrypt.BCrypt;

/** Sinh hash BCrypt hợp lệ cho 2 tài khoản mặc định. */
public class GenHash {
    public static void main(String[] args) {
        for (String[] p : new String[][]{{"admin", "admin123"}, {"staff", "staff123"}}) {
            String hash = BCrypt.hashpw(p[1], BCrypt.gensalt(10));
            System.out.printf("INSERT users (username, password_hash, full_name, role, active) VALUES ('%s', '%s', '%s', '%s', 1);%n",
                    p[0], hash,
                    p[0].equals("admin") ? "Quản trị viên" : "Nhân viên kho",
                    p[0].equals("admin") ? "ADMIN" : "STAFF");
            // verify ngay
            System.out.println("-- verify " + p[0] + ": " + BCrypt.checkpw(p[1], hash));
        }
    }
}
