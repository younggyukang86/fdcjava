import java.sql.*;
import java.text.DecimalFormat;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

public class Main {

    public static void main(String[] args) throws Exception {
        while(true) {
            System.out.println("명렁어를 입력하여 주세요.");
            System.out.println("1 : 테이블 생성");
            System.out.println("2 : 생성한 테이블에 데이터 입력");
            System.out.println("3 : 테이블 데이터 조회");
            System.out.println("4 : 종료");

            int num1 = 0;
            Scanner sc = new Scanner(System.in);
            num1 = sc.nextInt();

            if (num1 == 4) {
                break;
            }

            AtomicLong start = null;
            long end = 0;
            switch (num1) {
                case 1:
                    // 테이블 생성
                    deleteTable();
                    createTable();
                    break;
                case 2 :
                    // 생성한 테이블에 데이터 입력
                    deleteData();
                    int num2 = 0;
                    System.out.println("생성할 데이터 개수 -> 최소 1개 이상 입력");
                    num2 = sc.nextInt();

                    start = new AtomicLong(System.currentTimeMillis());
                    addData(num2);
                    //statementAddData(num2);
                    end = System.currentTimeMillis();
                    System.out.println("수행시간 : " + new DecimalFormat("###.0").format((end - start.get()) / 1000.0) + " 초");
                    break;
                case 3:
                    // 테이블 데이터 조회
                    start = new AtomicLong(System.currentTimeMillis());
                    List<User> list = getData();
                    end = System.currentTimeMillis();
                    System.out.println("수행시간 : " + new DecimalFormat("###.0").format((end - start.get()) / 1000.0) + " 초");
                    break;
                default:
                    break;
            }
        }
    }

    private static Connection getConnection() throws Exception {
        Connection connection = null;
        String server = "localhost:5432";
        String database = "test";
        String option = "";
        String id = "kangside21";
        String pwd ="kangside21";

        try {
            Class.forName("org.postgresql.Driver");
        } catch (Exception e) {
            System.err.println("load 오류 : " + e.getMessage());
            e.printStackTrace();
        }

        try {
            connection = DriverManager.getConnection("jdbc:postgresql://" + server + "/" + database + option, id, pwd);
            connection.setAutoCommit(false);
        } catch (Exception e) {
            System.err.println("연결오류 : " + e.getMessage());
            e.printStackTrace();
            throw new Exception("연결오류 : " + e.getMessage());
        }

        return connection;
    }

    private static void closeConnection(Connection connection) {
        try {
            if (connection != null) {
                connection.close();
            }
        } catch (Exception e) {
            connection = null;
        }
    }

    private static void addData(Integer max) throws Exception {
        if (max == null || max < 1) {
            max = 100000;
        }

        Connection connection = null;
        PreparedStatement pstmt = null;

        try {
            connection = getConnection();

            String query = "INSERT INTO test.\"user\" (\"NAME\", \"COMMENT\", \"NUMBER1\", \"NUMBER2\"";
            for (int i = 1; i <= 100; i++) {
                query += ",\"C" + i + "\"";
            }
            query += ") VALUES (?, ?, ?, ?";
            for (int i = 1; i <= 100; i++) {
                query += ",?";
            }
            query += ")";

            pstmt = connection.prepareStatement(query);

            for (int i = 0; i < max; i++) {
                String name = randomHangulName();
                String comment = "데이터 번호+" + Integer.toString(i + 1);
                String number1 = i + 1 + ".456";
                String number2 = i + 1 + "23.789";

                pstmt.setString(1, name);
                pstmt.setString(2, comment);
                pstmt.setDouble(3, Double.parseDouble(number1));
                pstmt.setDouble(4, Double.parseDouble(number2));
                for (int j = 1; j <= 100; j++) {
                    pstmt.setString(j + 4, randomName());
                }

                pstmt.addBatch();
                pstmt.clearParameters();

                if ((i % 10000) == 0) {
                    // 10000건 단위로 커밋
                    pstmt.executeBatch();
                    pstmt.clearBatch();
                    connection.commit();
                }
            }

            pstmt.executeBatch();
            pstmt.clearBatch();
            connection.commit();
        } catch (Exception e) {
            e.printStackTrace();

            try {
                connection.rollback();
            } catch (SQLException sqlEx) {
                sqlEx.printStackTrace();
            }
        } finally {
            closeConnection(connection);
            pstmt.close();
        }
    }

    public static void statementAddData(Integer max) throws Exception {
        if (max == null || max < 1) {
            max = 100000;
        }

        Connection connection = null;
        Statement stmt = null;

        try {
            connection = getConnection();

            String insertQuery = "INSERT INTO test.\"user\" (\"NAME\", \"COMMENT\", \"NUMBER1\", \"NUMBER2\"";
            for (int i = 1; i <= 100; i++) {
                insertQuery += ",\"C" + i + "\"";
            }
            insertQuery += ") VALUES (";

            stmt = connection.createStatement();

            for (int i = 0; i < max; i++) {
                String query = insertQuery;
                String name = randomHangulName();
                String comment = "데이터 번호+" + Integer.toString(i + 1);
                String number1 = i + 1 + ".456";
                String number2 = i + 1 + "23.789";

                query += "'" + name + "','" + comment + "'," + number1 + "," + number2;
                for (int j = 1; j <= 100; j++) {
                    query += ",'" + randomName() + "'";
                }
                query += ");";

                stmt.executeUpdate(query);

                if ((i % 10000) == 0) {
                    // 10000건 단위로 커밋
                    stmt.close();
                    connection.commit();
                    stmt = connection.createStatement();
                }
            }

            stmt.close();
            connection.commit();
        } catch (Exception e) {
            e.printStackTrace();

            try {
                connection.rollback();
            } catch (SQLException sqlEx) {
                sqlEx.printStackTrace();
            }
        } finally {
            closeConnection(connection);
            stmt.close();
        }
    }

    private static void deleteData() throws Exception {
        Connection connection = null;
        PreparedStatement pstmt = null;

        try {
            connection = getConnection();

            String query = "TRUNCATE TABLE test.\"user\"";

            pstmt = connection.prepareStatement(query);
            pstmt.executeUpdate();
            connection.commit();
        } catch (Exception e) {
            e.printStackTrace();

            try {
                connection.rollback();
            } catch (SQLException sqlEx) {
                sqlEx.printStackTrace();
            }
        } finally {
            closeConnection(connection);
            pstmt.close();
        }
    }

    private static List<User> getData() throws Exception {
        //List<Map<String, Object>> list = new ArrayList<>();
        List<User> list = new ArrayList<>();

        Connection connection = null;
        Statement stmt = null;

        try {
            connection = getConnection();

            String sql = "SELECT * FROM test.\"user\"";

            stmt = connection.createStatement();
            ResultSet rs = stmt.executeQuery(sql);

            while (rs.next()) {
                /*Map<String, Object> user = new HashMap<>();
                user.put("ID", rs.getInt("ID"));
                user.put("NAME", rs.getString("NAME"));
                user.put("COMMENT", rs.getString("COMMENT"));
                user.put("NUMBER1", rs.getDouble("NUMBER1"));
                user.put("NUMBER2", rs.getDouble("NUMBER2"));
                user.put("CREATED_DATE", rs.getTimestamp("CREATED_DATE"));
                for (int i = 1; i <= 100; i++) {
                    user.put("C"+i, rs.getString("C"+i));
                }*/

                User user = User.builder()
                        .id(rs.getInt("ID"))
                        .name(rs.getString("NAME"))
                        .comment(rs.getString("COMMENT"))
                        .number1(rs.getDouble("NUMBER1"))
                        .number2(rs.getDouble("NUMBER2"))
                        .createdDate(rs.getTimestamp("CREATED_DATE"))
                        .c1(rs.getString("C1"))
                        .c2(rs.getString("C2"))
                        .c3(rs.getString("C3"))
                        .c4(rs.getString("C4"))
                        .c5(rs.getString("C5"))
                        .c6(rs.getString("C6"))
                        .c7(rs.getString("C7"))
                        .c8(rs.getString("C8"))
                        .c9(rs.getString("C9"))
                        .c10(rs.getString("C10"))
                        .c11(rs.getString("C11"))
                        .c12(rs.getString("C12"))
                        .c13(rs.getString("C13"))
                        .c14(rs.getString("C14"))
                        .c15(rs.getString("C15"))
                        .c16(rs.getString("C16"))
                        .c17(rs.getString("C17"))
                        .c18(rs.getString("C18"))
                        .c19(rs.getString("C19"))
                        .c20(rs.getString("C20"))
                        .c21(rs.getString("C21"))
                        .c22(rs.getString("C22"))
                        .c23(rs.getString("C23"))
                        .c24(rs.getString("C24"))
                        .c25(rs.getString("C25"))
                        .c26(rs.getString("C26"))
                        .c27(rs.getString("C27"))
                        .c28(rs.getString("C28"))
                        .c29(rs.getString("C29"))
                        .c30(rs.getString("C30"))
                        .c31(rs.getString("C31"))
                        .c32(rs.getString("C32"))
                        .c33(rs.getString("C33"))
                        .c34(rs.getString("C34"))
                        .c35(rs.getString("C35"))
                        .c36(rs.getString("C36"))
                        .c37(rs.getString("C37"))
                        .c38(rs.getString("C38"))
                        .c39(rs.getString("C39"))
                        .c40(rs.getString("C40"))
                        .c41(rs.getString("C41"))
                        .c42(rs.getString("C42"))
                        .c43(rs.getString("C43"))
                        .c44(rs.getString("C44"))
                        .c45(rs.getString("C45"))
                        .c46(rs.getString("C46"))
                        .c47(rs.getString("C47"))
                        .c48(rs.getString("C48"))
                        .c49(rs.getString("C49"))
                        .c50(rs.getString("C50"))
                        .c51(rs.getString("C51"))
                        .c52(rs.getString("C52"))
                        .c53(rs.getString("C53"))
                        .c54(rs.getString("C54"))
                        .c55(rs.getString("C55"))
                        .c56(rs.getString("C56"))
                        .c57(rs.getString("C57"))
                        .c58(rs.getString("C58"))
                        .c59(rs.getString("C59"))
                        .c60(rs.getString("C60"))
                        .c61(rs.getString("C61"))
                        .c62(rs.getString("C62"))
                        .c63(rs.getString("C63"))
                        .c64(rs.getString("C64"))
                        .c65(rs.getString("C65"))
                        .c66(rs.getString("C66"))
                        .c67(rs.getString("C67"))
                        .c68(rs.getString("C68"))
                        .c69(rs.getString("C69"))
                        .c70(rs.getString("C70"))
                        .c71(rs.getString("C71"))
                        .c72(rs.getString("C72"))
                        .c73(rs.getString("C73"))
                        .c74(rs.getString("C74"))
                        .c75(rs.getString("C75"))
                        .c76(rs.getString("C76"))
                        .c77(rs.getString("C77"))
                        .c78(rs.getString("C78"))
                        .c79(rs.getString("C79"))
                        .c80(rs.getString("C80"))
                        .c81(rs.getString("C81"))
                        .c82(rs.getString("C82"))
                        .c83(rs.getString("C83"))
                        .c84(rs.getString("C84"))
                        .c85(rs.getString("C85"))
                        .c86(rs.getString("C86"))
                        .c87(rs.getString("C87"))
                        .c88(rs.getString("C88"))
                        .c89(rs.getString("C89"))
                        .c90(rs.getString("C90"))
                        .c91(rs.getString("C91"))
                        .c92(rs.getString("C92"))
                        .c93(rs.getString("C93"))
                        .c94(rs.getString("C94"))
                        .c95(rs.getString("C95"))
                        .c96(rs.getString("C96"))
                        .c97(rs.getString("C97"))
                        .c98(rs.getString("C98"))
                        .c99(rs.getString("C99"))
                        .c100(rs.getString("C100"))
                        .build();

                list.add(user);
            }


        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            closeConnection(connection);
            stmt.close();
        }

        return list;
    }

    private static void createTable() throws Exception {
        String sql = "CREATE TABLE test.user (";
        sql += "\"ID\" int4 generated always as identity NOT NULL,";
        sql += "\"NAME\" varchar(100) NOT NULL,";
        sql += "\"COMMENT\" varchar(500),";
        sql += "\"NUMBER1\" double precision NOT NULL,";
        sql += "\"NUMBER2\" numeric NOT NULL,";
        for (int i = 1; i <= 100; i++) {
            sql += "\"C"+i+"\" varchar(100) NOT NULL,";
        }
        sql += "\"CREATED_DATE\" timestamp NOT NULL default current_timestamp,";
        sql += "CONSTRAINT user_pkey PRIMARY KEY (\"ID\"));";

        Connection connection = null;
        PreparedStatement pstmt = null;

        try {
            connection = getConnection();
            pstmt = connection.prepareStatement(sql);
            pstmt.executeUpdate();
            connection.commit();

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            closeConnection(connection);
            pstmt.close();
        }
    }

    private static void deleteTable() throws Exception {
        String sql = "DROP TABLE test.user;";
        Connection connection = null;
        PreparedStatement pstmt = null;

        try {
            connection = getConnection();
            pstmt = connection.prepareStatement(sql);
            pstmt.executeUpdate();
            connection.commit();
        } catch (Exception e) {

        } finally {
            closeConnection(connection);
            pstmt.close();
        }

    }

    private static String randomHangulName() {
        List<String> lastName = Arrays.asList("김","이","박","최","정","강","조","윤","장","임","한","오","서","신","권","황","안","송","류","전","홍","고","문","양","손","배","조","백","허","유","남","심","노","정","하","곽","성","차","주","우","구","신","임","나","전","민","유","진","지","엄","채","원","천","방","공","강","현","함","변","염","양","변","여","추","노","도","소","신","석","선","설","마","길","주","연","방","위","표","명","기","반","왕","금","옥","육","인","맹","제","모","장","남","탁","국","여","진","어","은","편","구","용");
        List<String> firstName = Arrays.asList("가","강","건","경","고","관","광","구","규","근","기","길","나","남","노","누","다","단","달","담","대","덕","도","동","두","라","래","로","루","리","마","만","명","무","문","미","민","바","박","백","범","별","병","보","빛","사","산","상","새","서","석","선","설","섭","성","세","소","솔","수","숙","순","숭","슬","승","시","신","아","안","애","엄","여","연","영","예","오","옥","완","요","용","우","원","월","위","유","윤","율","으","은","의","이","익","인","일","잎","자","잔","장","재","전","정","제","조","종","주","준","중","지","진","찬","창","채","천","철","초","춘","충","치","탐","태","택","판","하","한","해","혁","현","형","혜","호","홍","화","환","회","효","훈","휘","희","운","모","배","부","림","봉","혼","황","량","린","을","비","솜","공","면","탁","온","디","항","후","려","균","묵","송","욱","휴","언","령","섬","들","견","추","걸","삼","열","웅","분","변","양","출","타","흥","겸","곤","번","식","란","더","손","술","훔","반","빈","실","직","흠","흔","악","람","뜸","권","복","심","헌","엽","학","개","롱","평","늘","늬","랑","얀","향","울","련");

        Collections.shuffle(lastName);
        Collections.shuffle(firstName);

        return lastName.get(0) + firstName.get(0) + firstName.get(1);
    }

    private static String randomName() {
        Random random = new Random();
        int length = random.nextInt(5) + 5;

        StringBuffer newWord = new StringBuffer();
        for (int i = 0; i < length; i++) {
            int mixed = random.nextInt(3);
            switch(mixed) {
                case 0:
                    newWord.append(randomWord("lower", 1));
                    break;
                case 1:
                    newWord.append(randomWord("upper", 1));
                    break;
                case 2:
                    newWord.append(randomWord("number", 1));
                    break;
                default:
                    break;
            }
        }

        return newWord.toString();
    }

    private static String randomWord(String selectCase, int length) {
        String result = "";

        if (selectCase == null) {
            selectCase = "";
        }

        switch (selectCase.toLowerCase()) {
            case "lower":
                for (int i = 0; i < length; i++) {
                    char lowerCh = (char) ((int) (Math.random() * 25) + 97);
                    result += lowerCh;
                }
                break;
            case "upper":
                for (int i = 0; i < length; i++) {
                    char upperCh = (char) ((int) (Math.random() * 25) + 65);
                    result += upperCh;
                }
                break;
            case "number":
                for (int i = 0; i < length; i++) {
                    char ch = (char) ((int) (Math.random() * 10) + 48);
                    result += ch;
                }
                break;
            default:
                break;
        }

        return result;
    }
}