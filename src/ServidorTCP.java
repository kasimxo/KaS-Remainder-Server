
//ServidorTCP.java
import java.io.*;
import java.net.*; 
import java.util.*;
import java.util.Map.Entry;

public class ServidorTCP {
	
	// Guardamos una coleccion de tipo mapa con K, V
	// Esto permite evitar usuarios duplicados
	//
	// 		K (String) nombre de usuario
	// 		V (Date) el momento de la conexión o última conexión.
	// Esto permite luego evaluar cuando ha sido la última conexión del usuario y cerrarla si queremos
	public static Map<String, Date> usuariosConectados;
	//La longitud del array de bytes tiene que ser suficiente para dar cabida a las peticiones
	public static byte[] pck;
	
	//Este mapa filtra por usuario y después por fecha para saber los antiguos, por ultimo recupera la cadena, el mensaje
	//Al tener esta estructura un usuario puede tener mas de un mensaje para la misma fecha
	public static Map<String, Map<Calendar, List<String>>> recordatorios;
	
	public static String buffer;
	public static int bufferN;
	

	public static void main(String[] args) {
		
		//Creamos una tarea que cada X tiempo lista a los usuarios conectados 
		TimerTask listarUsuarios = new ConnectedUsers();
		Timer timer = new Timer(true);
		timer.scheduleAtFixedRate(listarUsuarios, 0, 5000);
		
		
		
		usuariosConectados = new HashMap<String, Date>();
		recordatorios = new HashMap<String, Map<Calendar, List<String>>>();
		
		buffer = "";
		
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
				// Reseteamos los buffers
				buffer = "";
				bufferN = 0;
				
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
				 
				 byte[] pcc = new byte[10000];
				 dis.read(pcc);
				 
				 String pt = clean(pcc);
				 String respuesta = procesarPeticion(pt);
				 
				 // Escribimos el resultado
				 dos.write(respuesta.getBytes());
				 
				 // Cerramos los streams
				 dis.close();
				 //br.close();
				 dos.close();
				 sckt.close();
				 //ss.close();
				 // Registramos en salida estandard
				 System.out.println( "Cliente = " + direcc + ":" + puerto + "\tPeticion = " + pt); 
				 //ready = false;
			 }  catch(Exception e) {
				 System.err.println("Se ha producido la excepción : " +e);
				 e.printStackTrace();
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
		
		System.out.println("Se va a ejecutar la operación: "+operacion);
		
		switch (operacion) {
		case "Login":
			return login(operando);
		case "Exit":
			return exit(operando);
		case "GetN":
			return getN(operando);
		case "Get":
			return get(operando);
		case "Set":
			return set(operando);
		default:
			return "Operación no reconocida";
		}
		
	}
	
	// Crea un nuevo recordatorio para el usuario
	// El formato del recordatorio viene separado por ';'
	// nombreUsuario;segundosDelay;mensaje
	public static String set(String input) {
		try {
			String[] split = input.split(";");
			String nombreUsuario = split[0];
			String rawSegundos = split[1];
			String mensaje = split[2];
			Calendar time = Calendar.getInstance();
			time.add(Calendar.SECOND, Integer.parseInt(rawSegundos));
			Map<Calendar, List<String>> mapaUsuario = recordatorios.get(nombreUsuario);
			
			// Comprobamos que ya existiera algún recordatorio previo para el usuario (genérico)
			if(mapaUsuario == null) {
				mapaUsuario = new HashMap<Calendar, List<String>>();
				List<String> mensajesUsuario = new ArrayList<String>();
				mensajesUsuario.add(mensaje);
				mapaUsuario.put(time, mensajesUsuario);
				recordatorios.put(nombreUsuario, mapaUsuario);
				return "Se ha agregado un nuevo recordatorio";
			}
			
			List<String> recordatoriosUsuario = mapaUsuario.get(time);
			
			// Comprobamos que ya existiera algún recordatorio previo para el moment especificado
			if(recordatoriosUsuario == null) {
				recordatoriosUsuario = new ArrayList<String>();
				recordatoriosUsuario.add(mensaje);
				mapaUsuario.put(time, recordatoriosUsuario);
				recordatorios.put(nombreUsuario, mapaUsuario);
				return "Se ha agregado un nuevo recordatorio";
			}
			
			recordatoriosUsuario.add(mensaje);
			mapaUsuario.put(time, recordatoriosUsuario);
			recordatorios.put(nombreUsuario, mapaUsuario);
			
			
			
			return "Se ha agregado un nuevo recordatorio.";
		} catch (Exception e) {
			System.err.println("Se ha producido un error procesando la petición: Set="+input);
			e.printStackTrace();
			return "Se ha producido un error procesando la petición.";
		}
		
	}
	
	/**
	 * Únicamente devuelve el número de recordatorios sin leer de ese usuario
	 * Hace filtrado para contar únicamente aquellos recordatorios cuya hora ya ha pasado
	 * @param nombre
	 * @return
	 */
	public static String getN(String nombre) {
		if (recordatorios.containsKey(nombre)) {
			//return Integer.toString(recordatorios.get(nombre).size());
			
			bufferN = 0;
			
			Map<Calendar,List<String>> tot = recordatorios.get(nombre);
			
			if(tot == null) {
				return Integer.toString(bufferN);
			}
			
			tot.forEach( (K,V) -> {
				
				List<String> mensajes = null;
				
				if(K.before(Calendar.getInstance())) {
					mensajes = tot.get(K);
					if (mensajes != null) {
						bufferN += mensajes.size();
					}
				}
			}
					);
			
			return Integer.toString(bufferN);
			
		} else {
			return "0";
		}
		
	}
	
	public static String get(String nombre) {
		try {
			Map<Calendar,List<String>> tot = recordatorios.get(nombre);
			List<Calendar> deleteBuffer = new ArrayList<Calendar>();
			if(tot == null) {
				return "No hay ningún recordatorio";
			}
			String recordatoriosSinLeer = "";
			tot.forEach( (K,V) -> {	
				List<String> mensajes = null;
				if(K.before(Calendar.getInstance())) {
					mensajes = tot.get(K);
					if (mensajes != null) {
						for(String mensaje : mensajes) {
							buffer += mensaje + ";";
						}
					}
					deleteBuffer.add(K);
				}
			}
					);	
			if(buffer.length()>1) {
				buffer = buffer.substring(0, buffer.length()-1);
			}
			
			//Borramos los mensajes que se han enviado al usuario
			for (Calendar c : deleteBuffer) {
				recordatorios.get(nombre).remove(c);
			}
			
			return buffer;
		} catch (Exception e) {
			e.printStackTrace();
			return "No se han podido recuperar los recordatorios";
		}
	}
	
	
	public static String exit(String nombre) {
		User usuario = new User(nombre);
		boolean cerrado = cerrarSesion(usuario);
		if (cerrado) {
			return "Se ha cerrado la sesión.";
		} else {
			return "No se ha podido cerrar la sesión.";
		}
	}
	
	public static String login(String nombre) {
		User usuario = new User(nombre);
		if (usuariosConectados.get(usuario.getNombreUsuario()) != null) {
			return "Login incorrecto, ese usuario ya está conectado";
		} else {
			usuariosConectados.put(Integer.toString(usuario.hashCode()), new Date());
			System.out.println("Se ha conectado el usuario: "+usuario);
			return "Login correcto, iniciando sesión como '"+usuario+"'";
		}
	}
	
	
	/**
	 * La existencia de este método es el resultado de dos horas de volverme loco.
	 * Al convertir el byte[] a String, se añade el final de línea.
	 * Esto por algún motivo tenía problemas con la conversión al .hashCode 
	 * Provocaba que pese a usar la misma String como nombre de usuario, la comparación no diera resultado
	 * Seguro que existe algún método mas elegante de hacer esto pero por el momento funciona
	 * 
	 * Update 09/02/2024:
	 * Esto ya está absoleto, pero lo dejo para que se pueda observar mi locura
	 *  
	 * @param cadena
	 * @return
	 */
	public static String clean(String cadena) {
		String clean = "";
		String abc = "abcdefghijklmnñopqrstuvwxyz1234567890=;: ";
		for(int i = 0; i<cadena.length(); i++) {
			String test = ""+cadena.charAt(i);
			if(abc.contains(test.toLowerCase())) {
				clean += cadena.charAt(i);
			}
		}
		return clean;
	}
	
	
	/**
	 * Esta es la manera elegante de hacer lo de arriba
	 * @param input
	 * @return
	 */
	public static String clean(byte[] input) {
		String sol = "";
		for(byte by : input) {
			if (by != 0) {
				sol += (char) by;
			}
		}
		return sol;
	}
	
	
	public static boolean cerrarSesion(User usuario) {
		if (usuariosConectados.remove(Integer.toString(usuario.hashCode())) == null) {
			//Devolvemos error porque no se ha podido cerrar la sesion
			return false;
		} 
		return true;
	}

}
