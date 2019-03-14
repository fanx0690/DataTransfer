# data-transfer

This is a small project based on java
which is implement UDP protocol to transfer data

Sender is going to cut the file into small data datagrams each one has 124 byte 
and also it will add our more byte to the front of the datagram which is using for sequence number. 
Sender do not have a GUI, all input should include in command-line arguments.

Receiver has a GUI to type in IP address and port number. it  also has a "transfer" button to decide
when to start the transfer.

Suggest using command line to run this code.

