<%@ page language="java" contentType="text/html; charset=utf-8"
  pageEncoding="utf-8"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<!DOCTYPE html>
<html>
<head>
<script src="<c:url value="/resources/js/admin/ajax/fieldWeight.js" />"></script>
<link href="<c:url value="/resources/css/admin/fieldWeight.css" />" rel="stylesheet"></link>
<meta charset="UTF-8">
<title>Insert title here</title>
</head>
<body>
	<!--Start Breadcrumb-->
	<nav aria-label="breadcrumb" class="bg-dark">
    <a href="#" class="side-menu-control"> <i class="fas fa-angle-left"></i></a>
    <ol class="breadcrumb">
      <li class="breadcrumb-item indexAdminUIBreadcrumbLink" id="topbar1"></li>
      <li class="breadcrumb-item" id="topbar2"></li>
      <li class="breadcrumb-item active" aria-current="page" id="topbar3"></li>
    </ol>
  </nav>
	<!--End Breadcrumb-->
	<div class="col-sm-12"><span id="globalAnswer"></span></div><br/>
	<div class="col-sm-12"></div>
	<div class="box">
		<div class="box-header">
			<div class="box-name">
				<i class="fas fa-table"></i><span  id="qfname"></span>
			</div>
			<div class="box-icons">
			<a class="collapse-link"><i class="fas fa-chevron-up"></i></a>
			</div>
			<div class="no-move"></div>
		</div>
		<div id="qfBox" class="box-content">
			<form class="form-horizontal" role="form">
				<div class="form-group" >
				<div class="col-sm-1"></div>
				<div class="col-sm-3">
					<select id="selectqf" class="form-control" onchange="Javascript : selectQF()">
					<option></option>
					</select>
				</div>
				<div class="col-sm-2"></div>
				<div>
				<input type="number" min="0"id="weightqf" name="weightqf" class="col-sm-1">
				<div class="col-sm-1"></div>
				<button id="submitqf" name="submitqf" class="btn btn-primary btn-label-left  col-sm-2"></button>
				</div>
				<div class="col-sm-12"><span id="answerqf"></span></div>
				</div>
			</form>
		</div>
	</div>
	<div class="col-sm-12"></div>
	<div class="box">
		<div class="box-header">
			<div class="box-name">
				<i class="fas fa-table"></i><span id="pfname" ></span>
			</div>
			<div class="box-icons">
			<a class="collapse-link"><i class="fas fa-chevron-up"></i></a>
			</div>
			<div class="no-move"></div>
		</div>
		<div id="pfBox" class="box-content">
			<form class="form-horizontal" role="form">
			<div class="form-group" >
				<div class="col-sm-1"></div>
				<div class="col-sm-3">
					<select id="selectpf" class="form-control" onchange="Javascript : selectPF()">
					<option></option>
					</select>
				</div>
				<div class="col-sm-2"></div>
				<div>
				<input type="number" min="0" id="weightpf" name="weightpf" class="col-sm-1">
				<div class="col-sm-1"></div>
				<button id="submitpf" name="submitpf" class="btn btn-primary btn-label-left col-sm-2"></button>
				</div>
				<div class="col-sm-12"><span id="answerpf"></span></div>
				</div>
			</form>
			
		</div>
	</div>
	<div class="end-group">
			<button id="uprel" name="uprel" class="btn btn-primary btn-label-left col-sm-2"></button>
			<div class="col-sm-12"><span id="answeruprel"></span></div>
			</div>
</body>

