make:
	javac *.java

jar:
	mkdir jar
	cp Mkfs.java VFS.java VFSSync.java SJF.java BufferCache.java BufferCacheSync.java jar
	cp test[0-2].tr jar
	cp README jar
	cp ANSWERS.txt jar
	cd jar; jar -cfMv ../FileSystem.jar *.java test*.tr README ANSWERS.txt AUTHORS.txt
	rm -rf jar

turnin:
	/lusr/bin/turnin --submit naga86 lab4 FileSystem.jar

clean:
	rm *.class
	rm test_disk

test_file_system:
	od -A x1 -t x1 test_disk > check_file_system.out
	diff check_file_system.check check_file_system.out

run:
	make file_system
	java ProcessTrace

file_system:
	java Mkfs test_disk 128k

re_make:
	make clean
	make
	make run
