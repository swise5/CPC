# GENERAL IDEA: plot both the "normal" and "extreme" values
# spatial, temporal, social, by status

# histogram of the overall values
plotAverageVals <- function(filegroup, dirname){

	par(mar=c(3,3,3,3))
	par(mfrow=c(3,5))

	# SPATIAL
	
	s_data <- scan(file=paste(dirname, filegroup, "_1.txt", sep=""), what=double(1), sep=",")
	plotHisto(s_data, "Spatial")
	
	plot.new() # skip a position

	# SPATIO-TEMPORAL

	s_data <- scan(file=paste(dirname, filegroup, "_01.txt", sep=""), what=double(1), sep=",")
	plotHisto(s_data, "Spatio-Temporal")
	
	plot.new() # skip a position
	
	# TEMPORAL
	
	s_data <- scan(file=paste(dirname, filegroup, "_0.txt", sep=""), what=double(1), sep=",")
	plotHisto(s_data, "Temporal")

	# S-G

	plot.new()	# skip a position

	s_data <- scan(file=paste(dirname, filegroup, "_13.txt", sep=""), what=double(1), sep=",")
	plotHisto(s_data, "Spatio-Social")

	# S-T-G
	
	s_data <- scan(file=paste(dirname, filegroup, "_013.txt", sep=""), what=double(1), sep=",")
	plotHisto(s_data, "Spatio-Temporal-Social")

	# T-G

	s_data <- scan(file=paste(dirname, filegroup, "_03.txt", sep=""), what=double(1), sep=",")
	plotHisto(s_data, "Temporal-Social")

	plot.new() # skip a position
	
	# GROUP
	
	plot.new() # skip a position
	plot.new() # skip a position

	s_data <- scan(file=paste(dirname, filegroup, "_3.txt", sep=""), what=double(1), sep=",")
	plotHisto(s_data, "Social")
}



	
	plot.new() # skip a position
	
	# S-G

	plot.new()	# skip a position

	s_data <- scan(file=paste(dirname, filegroup, "_13.txt", sep=""), what=double(1), sep=",")
	plotHisto(s_data, "Spatio-Social")

	# S-T-G
	
	s_data <- scan(file=paste(dirname, filegroup, "_013.txt", sep=""), what=double(1), sep=",")
	plotHisto(s_data, "Spatio-Temporal-Social")

	# GROUP
	
	plot.new() # skip a position
	plot.new() # skip a position

	s_data <- scan(file=paste(dirname, filegroup, "_3.txt", sep=""), what=double(1), sep=",")
	plotHisto(s_data, "Social")	


# analysis-specific view of extreme values
plotExtremeVals <- function(filegroup, dirname, network){
	
	par(mar=c(3,3,3,3))
	par(mfrow=c(3,1))
	
	# TEMPORAL
	
	s_data <- read.table(file=paste(dirname, filegroup, "extremes_0.txt", sep=""))
	df <- data.frame(s_data)
	plotTime(df, "Temporal Extreme Values")
	
	# SPATIAL
	
	s_data <- read.table(file=paste(dirname, filegroup, "extremes_1.txt", sep=""))
	df <- data.frame(s_data)
	plotSpace(df, network)
		
}

# plots a histogram with logged y axis
plotHisto <- function(x, title){
	if(length(x) > 100){
		hist.data = hist(x, breaks="Scott", plot=FALSE) # max(length(x)/10, 3)
		hist.data$counts = log10(hist.data$counts)
		plot(hist.data, ylab='log(Count)', ylim=c(0,max(hist.data$counts)), xlim=c(-6000,6000), main=title)		
	}
	else{
		stripchart(x, method="stack", pch=19, main=title, xlim=c(-6000,6000), ylim=c(0,max(x)))
	}
}

# plot the change over time
plotTime <- function(x, title){
	plot(x[,1], x[,2], type='p', xlab="Time", ylab="Count", main=title)
}

# plots 
plotSpace <- function(x, network){

	dummy = network
	dummy@data <- merge(network@data, x, by.x="FID_1", by.y="V1")
	
	rbPal <- colorRampPalette(c('red','gray','blue'))
	parts <- as.numeric(cut(dummy@data$V2,breaks = 15))
	dummy@data$Col <- rbPal(15)[parts]
	dummy@data$Width <- abs(parts - 7)
	
	plot(dummy, col=dummy@data$Col, cex=dummy@data$Width)

}


# SETUP

dirfile <- "/Users/swise/workspace/CPC/data/OUTPUTFROMLEGION/extremes/"
fileprefix <- "base_cadMarch2011"
fileprefix <- "noRoles_cadMarch2011"
fileprefix <- "noCAD_cadMarch2010"

# cex for lines!!!!
camden <- readShapeLines(fn="/Users/swise/workspace/CPC/data/itn/camden_itn_buff100pl2.shp", proj4string=CRS("+proj=tmerc +lat_0=49 +lon_0=-2 +k=0.9996012717 +x_0=400000 +y_0=-100000 +ellps=airy +datum=OSGB36 +units=m +no_defs"))


png("/Users/swise/blah.png", width=1200, height=800, res=60)