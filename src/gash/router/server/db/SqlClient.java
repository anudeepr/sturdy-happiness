package gash.router.server.db;
import org.apache.commons.io.IOUtils;

import java.io.*;
import java.sql.*;

public class SqlClient {
    final String JDBC_DRIVER = "com.mysql.jdbc.Driver";
    final String DB_URL = "jdbc:mysql://localhost/cmpe275";

    //  Database credentials
    final String USER = "root";
    final String PASSWORD = "password";

    Connection connection = null;
    Statement stmt = null;
    PreparedStatement insertStatement = null;
    PreparedStatement readByFname = null;
    PreparedStatement readByFId = null;
    public SqlClient(){
        checkDependency();
        establishConnection();
        prepareStatements();
    }

    private void checkDependency(){
        try
        {
            Class.forName("com.mysql.jdbc.Driver");
            Class.forName("org.apache.commons.io.IOUtils");
        }
        catch (ClassNotFoundException e) {
            System.out.println("Missing Dependency !!!!!!!!!!!!!!!!!");
            e.printStackTrace();
        }
    }
    private void establishConnection(){
        try
        {
            connection = DriverManager.getConnection(DB_URL,USER,PASSWORD);
        }
        catch (SQLException e)
        {
            System.out.println(" Connection Failed! Check output console");
            e.printStackTrace();
        }
        if (connection != null) {
            System.out.println("You made it, take control your database now!");
        } else {
            System.out.println("Failed to make connection!");
        }
    }
    private void prepareStatements(){
        try{
            insertStatement = connection.prepareStatement("INSERT INTO FILES(CHUNK_ID, FILENAME, CONTENT) " +
                    "VALUES(?,?,?)");
            readByFname = connection.prepareStatement("SELECT CONTENT from FILES where FILENAME = ? LIMIT 1 ");
            readByFId =  connection.prepareStatement("SELECT CONTENT from FILES where ID = ? ");
        }
        catch (Exception e){
            System.out.println(" PrepareStatements failed !");
            e.printStackTrace();
        }

    }

    public void storefile(int chunck_id, String path, String filename){
        try{
            File file = new File(path);
            FileInputStream InputStream = new FileInputStream(file);
            System.out.println("Found the file .....");

            insertStatement.setInt(1, chunck_id);
            insertStatement.setString(2, filename);
            insertStatement.setBinaryStream(3, InputStream, (int) file.length());
            insertStatement.execute();
        }catch (Exception e){
            System.out.println("File store failed");
            e.printStackTrace();
        }

    }
    public void storefile(int chunck_id, InputStream inputStream, String filename){
        try{
            System.out.println("storing the file .....");

            insertStatement.setInt(1, chunck_id);
            insertStatement.setString(2, filename);
            insertStatement.setBinaryStream(3, inputStream);
            insertStatement.execute();
        }catch (Exception e){
            System.out.println("File store failed");
            e.printStackTrace();
        }

    }

    public byte[] getFile(String file_name){
        FileOutputStream fileOuputStream = null;
        byte[] res = new byte[0];
        try {
            readByFname.setString(1, file_name);
            ResultSet rs = readByFname.executeQuery();
            System.out.println("Executed query");
            InputStream IS;
            if(rs.next()) {
                IS = rs.getBinaryStream("CONTENT");
                //fileOuputStream = new FileOutputStream(target);
                //fileOuputStream.write(IOUtils.toByteArray(IS));
                fileOuputStream.close();
                return IOUtils.toByteArray(IS);
            }
        }
        catch (Exception e){
            System.out.println("File get failed");
            e.printStackTrace();
        }
        return res;
    }
    public byte[] getFile(int id){
        FileOutputStream fileOuputStream = null;
        byte[] res = new byte[0];
        try {
            readByFId.setInt(1, id);
            ResultSet rs = readByFname.executeQuery();
            System.out.println("Executed query");
            InputStream IS;
            if(rs.next()) {
                IS = rs.getBinaryStream("CONTENT");
                //fileOuputStream = new FileOutputStream(target);
                //fileOuputStream.write(IOUtils.toByteArray(IS));
                fileOuputStream.close();
                return IOUtils.toByteArray(IS);
            }
        }
        catch (Exception e){
            System.out.println("File get failed");
            e.printStackTrace();
        }
        return res;
    }

}

