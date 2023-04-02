<?xml version="1.0" encoding="UTF-8"?>
<!--
    Copyright (c) 2022 IBM Corporation and others.
    All rights reserved. This program and the accompanying materials
    are made available under the terms of the Eclipse Public License 2.0
    which accompanies this distribution, and is available at
    http://www.eclipse.org/legal/epl-2.0/
    
    SPDX-License-Identifier: EPL-2.0
   
    Contributors:
        IBM Corporation - initial API and implementation
 -->
<xsl:stylesheet version="1.0"
	xmlns:xsl="http://www.w3.org/1999/XSL/Transform">

	<xsl:output method="html" indent="yes" />

	<xsl:template match="stocks">
		<br />
		<h2>Stock Table</h2>
		<table id="stockTable" border="1" width="100%">
			<tr>
				<th style="text-align:left">Stock</th>
				<th style="text-align:left">Price</th>
			</tr>
			<xsl:for-each select="stock">
				<tr>
					<td>
						<i>
							<xsl:value-of select="name" />
						</i>
					</td>

					<td>
						<xsl:value-of select="price" />
					</td>
				</tr>
			</xsl:for-each>
		</table>
		<br />
		<br />
		<h2>Name Sorted Stock Table</h2>
		<table id="nameSortedStockTable" border="1" width="100%">
			<tr>
				<th style="text-align:left">Sorted Stock</th>
				<th style="text-align:left">Price</th>
			</tr>
			<xsl:for-each select="stock">
				<xsl:sort select="name" />
				<tr>
					<td>
						<i>
							<xsl:value-of select="name" />
						</i>
					</td>

					<td>
						<xsl:value-of select="price" />
					</td>
				</tr>
			</xsl:for-each>
		</table>
		<br />
		<br />
		<h2>Price Sorted Stock Table</h2>
		<table id="priceSortedStockTable" border="1" width="100%">
			<tr>
				<th style="text-align:left">Sorted Stock</th>
				<th style="text-align:left">Price</th>
			</tr>
			<xsl:for-each select="stock">
				<xsl:sort select="price" data-type="number" />
				<tr>
					<td>
						<i>
							<xsl:value-of select="name" />
						</i>
					</td>

					<td>
						<xsl:value-of select="price" />
					</td>
				</tr>
			</xsl:for-each>
		</table>
	</xsl:template>


</xsl:stylesheet>
