<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
	<meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
	<title>File Not Found</title>
	<link type='text/css' rel='stylesheet' href='${pageContext.request.contextPath}/css/bootstrap.min.css' media='all' />
</head>

<body>
	<div id="wrap">
		<div class="container">
			<img src="${pageContext.request.contextPath}/img/impc.jpg">
			<div class="page-header">
				<h1>Oops! An error has occurred.</h1>
			</div>
			<p class="lead">This could be due to, eg.</p>
			<ul>
				<li>A mis-typed address</li>
				<li>An out-of-date bookmark</li>
				<li>The page no longer exists</li>        
			</ul>
			<br >
		</div>
    </div>

	<div id="footer">
		<div class="container">
			<p class="muted credit"><a href="${pageContext.request.contextPath}">Click here to search IMPC.</a></p>
		</div>
	</div>

	<script type='text/javascript' src="http://ajax.googleapis.com/ajax/libs/jquery/1.7.2/jquery.min.js"></script>
    <script type='text/javascript' src='${pageContext.request.contextPath}/js/bootstrap/bootstrap.min.js'></script>
</body>
</html>