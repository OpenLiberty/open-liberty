sinon downloaded from sinonjs.org
http://sinonjs.org/

BSD license (http://dojotoolkit.org/license)


How I got this to work:
1. Download from above
2. Copy runner.html and create runner-sinon.html which includes sinon. Add to runner-sinon.html:

		<!-- Edit 1: include sinon -->
		<script type="text/javascript" src="sinon-1.7.3.js">
		
		<!-- Edit 2: force dojo to use non-native XHR -->
		<script>
			data-dojo-config="has: { native-xhr2: false }";
		</script>
