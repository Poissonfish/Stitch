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
names = "trumpfine.txt"
num = fread(names) %>% as.matrix()
col = colorRampPalette(c("black", "white"), space = "rgb")  
corrplot(num, is.corr = FALSE, method = "shade", shade.col = NA, tl.col = "black", tl.srt = 45, col = col(255))
