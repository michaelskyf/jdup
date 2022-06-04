import java.nio.ByteBuffer;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.DecimalFormat;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.ArrayList;
import java.io.FileInputStream;

class Global
{
	public static HashMap<Long, ArrayList<String>> map; /* Key: file size | Value: List of strings */
}

class FileWalker
	extends SimpleFileVisitor<Path>
{
	@Override
	public FileVisitResult visitFile(Path path, BasicFileAttributes attr)
	{
		if(!attr.isRegularFile())
			return FileVisitResult.CONTINUE;

		File file = new File(path.toString());
		Long size = file.length();

		if(Global.map.containsKey(size))
		{
			/* Collision */
			ArrayList<String> list = Global.map.get(size);
			list.add(path.toString());
		}
		else
		{
			/* No collision */
			ArrayList<String> list = new ArrayList<String>();
			list.add(path.toString());

			Global.map.put(size, list);
		}

		return FileVisitResult.CONTINUE;
	}

	@Override
	public FileVisitResult visitFileFailed(Path file, IOException x)
	{
		System.err.printf("Cannot access '%s': %s\n", file.toString(), x.toString());

		return FileVisitResult.CONTINUE;
	}
}

class Jdup
{
	private static String HRSize(float size)
	{
		String str = new String();
		DecimalFormat dfmt = new DecimalFormat();
		dfmt.setMaximumFractionDigits(2);

		String unit;

		if(size < 1024)
		{
			unit = "B";
		}
		else if(size < 1024*1024)
		{
			size /= 1024;
			unit = "KB";
		}
		else if(size < 1024*1024*1024)
		{
			size /= 1024*1024;
			unit = "MB";
		}
		else if(size < 1024*1024*1024*1024)
		{
			size /= 1024*1024*1024;
			unit = "GB";
		}
		else
		{
			size /= 1024*1024*1024*1024;
			unit = "TB";
		}

		str = dfmt.format(size) + " " + unit;
		return str;
	}

	private static byte[] file_get_md5(String path)
		throws IOException, NoSuchAlgorithmException
	{
		FileInputStream file = new FileInputStream(path);
		MessageDigest md = MessageDigest.getInstance("MD5");
		byte[] buffer = new byte[1024 * 1024]; /* 1MB buffer */

		int read_bytes;
		do
		{
			read_bytes = file.read(buffer);
			if(read_bytes > 0)
			{
				md.update(buffer, 0, read_bytes);
			}
		}
		while(read_bytes != -1);

		file.close();

		return md.digest();
	}

	private static void print_duplicates()
	{
		/* Steps:
		 *
		 * 1. Calculate hashes of each file in the hashmap's bucket
		 * 2. If there are more than one hashes, print all of them
		 *
		 */

		Global.map.forEach((size, list)
		->{
			if(list.size() > 1)
			{
				/* Create a hashmap with md5 as key */
				HashMap<ByteBuffer, ArrayList<String>> map = new HashMap<ByteBuffer, ArrayList<String>>();

				list.forEach((path)
				->{
					ByteBuffer md5;
					try{
						md5 = ByteBuffer.wrap(file_get_md5(path));
					}
					catch(Exception x)
					{
						System.err.printf("Failed to calculate md5 of file '%s': %s\n", path, x);
						return;
					}

					if(map.containsKey(md5))
					{
						ArrayList<String> path_list = map.get(md5);
						path_list.add(path);
					}
					else
					{
						ArrayList<String> path_list = new ArrayList<String>();
						path_list.add(path);

						map.put(md5, path_list);
					}
				});

				map.forEach((hash, path_list)
				->{
					if(path_list.size() > 1)
					{
						System.out.printf("\n\nFound duplicate files with size %s:\n", HRSize(size));
						path_list.forEach((path)
						->{
							System.out.printf("%s\n", path);
						});
					}
				});
			}
		});
	}

	public static void main(String[] args)
	{
		/* Steps:
                 *
                 * 1. Walk file tree and add filenames to Global.map
                 * 2. Calculate hashes
                 * 3. If hashes match, print all files with matching hashes
                 *
		 */

		/* Init Global */
		Global.map = new HashMap<>();

		/* Walk the file tree */
		if(args.length != 0)
		{
			Path path;
			for(int i = 0; i < args.length; i++)
			{
				path = Paths.get(args[i]);
				FileWalker fw = new FileWalker();

				try
				{
					Files.walkFileTree(path, fw);
				}
				catch(IOException x)
				{
					System.err.printf("IOException: %s\n", x.getMessage());
				}
			}
		}
		else
		{
			FileWalker fw = new FileWalker();

			try
			{
				Files.walkFileTree(Path.of("."), fw);
			}
			catch(IOException x)
			{
				System.err.printf("IOException: %s\n", x.getMessage());
			}
		}

		/* Calculate hashes and print filenames */
		print_duplicates();
	}
}
