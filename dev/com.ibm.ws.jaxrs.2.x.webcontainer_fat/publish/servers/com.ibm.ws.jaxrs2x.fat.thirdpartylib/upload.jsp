<html>
<body>
	<h1>JAX-RS Upload Form</h1>

	<form action="http://localhost:8013/MultipartServer/multipart/resource/uploadFile" method="post" enctype="multipart/form-data">

		<p>
			Select a file : <input type="file" name="uploadedFile" size="50" />
		</p>

		<input type="submit" value="Upload It" />
	</form>

</body>
</html>