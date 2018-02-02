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

# Text matrix to Imgas
names = "out/trump_fine1p10_by"
series = c("5k", "10k", "30k", "50k", "100k", "500k", "1m", "3m", "5m", "10m")
for (string in paste0(names, series)) {
	num = fread(paste0(string, ".txt")) %>% as.matrix()
	col = colorRampPalette(c("black", "white"), space = "rgb")
	png(paste0(string, ".png"), width = 800, height = 800, units = "px")
	corrplot(num, is.corr = FALSE, method = "shade", shade.col = NA, tl.col = "black", tl.srt = 45, col = col(255))
	dev.off()
}

# organize data
data = data.table()
for (f in c(1, 2, 3)) {
	for (p in c(5, 10, 20, 50)) {
		sprintf("%d %d", f, p)
		fine = rep(f, 10)
		pixel = rep(p, 10)
		temp = sprintf("out/trump_fine%dp%d.rmse", f, p) %>% fread()
		temp = cbind(fine, pixel, temp)
		data = rbind(data, temp)
	}
}
# data = cbind(data, grp = paste0(data$fine, data$pixel))
data[nIter >= 10000 & pixel == 10]

g = ggplot(data[nIter >= 50000 & pixel == 10], aes(x = nIter, y = RMSE, color = factor(fine), group = fine)) +
 geom_point(alpha = .4, size = 3) + geom_line()
# g = ggplot(data[data$pixel == 5], aes(x = nIter, y = RMSE, color = factor(fine), group = fine)) + geom_point() + geom_line()
ggsave(plot = g, file = "p10.png", height = 3, width = 8)


g = ggplot(data[nIter <= 500000 & fine == 1], aes(x = nIter, y = RMSE, color = factor(pixel), group = pixel)) +
 geom_point(alpha = .4, size = 3) + geom_line()
g
ggsave(plot = g, file = "f1.png", height = 3, width = 8)
