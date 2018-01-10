library(raster)
library(rgdal)
library(data.table)
library(magrittr)
library(ggplot2)
library(corrplot)

# Img to Text matrix
names = "sTrump"
data = brick(paste0(names, ".jpg")) %>% getValues()
size = dim(data)[1] %>% sqrt
bw = (data[,1] * 0.21 + data[,2] * 0.72 + data[,3] * 0.07) / 255 %>%
	as.matrix() %>%	
	matrix(ncol = size, nrow = size)  
col = colorRampPalette(c("black", "white"), space = "rgb")  
corrplot(t(bw), is.corr = FALSE, method = "shade", shade.col = NA, tl.col = "black", tl.srt = 45, col = col(255))
write.table(x = t(bw), file = paste0(names, ".txt"), quote = F, row.names = F, col.names = F, sep = "\t")

# Text matrix to Img
names = "trump_fine3_by"
series = c("5k", "10k", "30k", "50k", "100k", "500k", "1m", "3m", "5m", "10m")
for (string in paste0(names, series)) {
	num = fread(paste0(string, ".txt")) %>% as.matrix()
	col = colorRampPalette(c("black", "white"), space = "rgb")  
	png(paste0(string, ".png"), width = 800, height = 800, units = "px")
	corrplot(num, is.corr = FALSE, method = "shade", shade.col = NA, tl.col = "black", tl.srt = 45, col = col(255))
	dev.off()
}
