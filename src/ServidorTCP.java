
//ServidorTCP.java
import java.io.*;
import java.net.*; 
import java.util.*;

public class ServidorTCP {
	
	public static HashSet<String> usuariosConectados;
	//public static List<String> usuariosConectados;

	public static void main(String[] args) {
		
		//Creamos una tarea que cada X tiempo lista a los usuarios conectados 
		TimerTask listarUsuarios = new ConnectedUsers();
		Timer timer = new Timer(true);
		timer.scheduleAtFixedRate(listarUsuarios, 0, 15000);
		
		//Utilizamos hashset para evitar duplicados
		usuariosConectados = new HashSet<String>();
		//usuariosConectados = new ArrayList<String>();
		
		boolean ready = true;
		
		// Primero indicamos la dirección IP local 
		try {
			System.out.println("LocalHost = " + InetAddress.getLocalHost().toString()); 
		} catch (UnknownHostException uhe)
		{
			System.err.println("No puedo saber la dirección IP local : " + uhe); 
		}
		
		// Abrimos un "Socket de Servidor" TCP en el puerto 1234.
		ServerSocket ss = null; 
		
		try {
			ss = new ServerSocket(1234);  
		} catch (IOException ioe)
		{
			System.err.println("Error al abrir el socket de servidor : " + ioe);
			System.exit(-1);  
		}
		
		//Esta es la peticion del cliente
		String peticion;
		long salida;
		
		
		
		// Bucle infinito 
		while(ready)
		{ 
			try
			 { 
				// Esperamos a que alguien se conecte a nuestroSocket
				//Como esto nos bindea con un cliente específico, lo metemos dentro del while para poder cerrarlo al servir al cliente y volver a liberar el servicio
				 Socket sckt = ss.accept();
				 // Extraemos los Streams de entrada y de salida
				 DataInputStream dis = new DataInputStream(sckt.getInputStream());
				 DataOutputStream dos = new DataOutputStream(sckt.getOutputStream());
				 
				
				 // Podemos extraer información del socket
				 // Nº de puerto remoto
				 int puerto = sckt.getPort();
				 // Dirección de Internet remota
				 InetAddress direcc = sckt.getInetAddress();
				 
				 // Leemos datos de la peticion
				 //peticion = br.readLine();
				 System.out.println("Voy a leer el input");
				 
				 //La longitud del array de bytes tiene que ser suficiente para dar cabida a las peticiones
				 byte[] pck = new byte[10000];
				 dis.read(pck);
						 //dis.readAllBytes();
				 System.out.println("Leyendo input");
				 peticion = new String(pck);
				 System.out.println(peticion);
				 
				 String respuesta = procesarPeticion(peticion);
				 
				 // Escribimos el resultado
				 dos.write(respuesta.getBytes());
				 
				 // Cerramos los streams
				 dis.close();
				 //br.close();
				 dos.close();
				 sckt.close();
				 //ss.close();
				 // Registramos en salida estandard
				 System.out.println( "Cliente = " + direcc + ":" + puerto + "\tPeticion = " + peticion); 
				 //ready = false;
			 }  catch(Exception e) {
				 System.err.println("Se ha producido la excepción : " +e);
			 }
		 }
		try {
			ss.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}
	
	/**
	 * Procesamos la petición recibida.
	 * Todas tinen la estructura "operación"="operando"
	 * @param peticion
	 * @return
	 */
	public static String procesarPeticion(String peticion) {
		
		String[] separada = peticion.split("=");
		String operacion = separada[0];
		String operando = separada[1];
		
		switch (operacion) {
		case "Login":
			boolean conectado = usuariosConectados.add(operando);
			if (conectado) {
				return "Login correcto, iniciando sesión como '"+operando+"'";
			} else {
				return "Login incorrecto, ese usuario ya está conectado";
			}
		case "Exit":
			boolean cerrado = cerrarSesion(operando);
			if (cerrado) {
				return "Se ha cerrado la sesión.";
			} else {
				return "No se ha podido cerrar la sesión.";
			}
		default:
			return "Operación no reconocida";
		}
		
	}
	
	public static boolean cerrarSesion(String nombreUsuario) {
		return usuariosConectados.remove(nombreUsuario);
	}

}
