
// import java.io.FileOutputStream;
// import java.io.OutputStream;

// abstract  class  abst{
//     // Java Program to demonstrate Reading Writing Binary Data
// // with InputStream/OutputStream
 
//     public static void main(String[] args) {
//         try {
//             // Writing binary data to a file using OutputStream
//             byte[] data = {(byte) 0xe0, 0x4f, (byte) 0xd0, 0x20, (byte) 0xea};
//             OutputStream os = new FileOutputStream("data.bin");
//             os.write(data);
//             os.close();

//             // Reading binary data from a file using InputStream
//             InputStream is = new FileInputStream("data.bin");
//             byte[] newData = new byte[5];

//             is.read(newData);
//             is.close();

//             // Printing the read data
//             for (byte b : newData) {
//                 System.out.print(b+" ");
//             }
//         } catch (IOException e) {
//             e.printStackTrace();
//         }
//     }
// }
