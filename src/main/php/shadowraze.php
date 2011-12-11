<?php

error_reporting(E_ALL);

$address = "mc.shadowraze.net";
$port = 6666;
$i = $_GET["query"];
if (!is_null($i)) {
    connect($address, $port, $i);
}

function connect($address, $port, $i) {
    $socket = socket_create(AF_INET, SOCK_STREAM, SOL_TCP);
    socket_connect($socket, $address, $port);
    socket_write($socket, $i);
    socket_close($socket);
}

?>
