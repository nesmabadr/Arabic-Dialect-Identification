for file in *.wav;do
    outfile="spect/${file%.*}.png"
    sox "$file" -n spectrogram -o "$outfile"
done
