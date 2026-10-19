# SRMusic Android UI/UX & Architecture Instructions

1. **No Grey Box Ripple**: All interactive elements and clickable cards MUST explicitly set `indication = null` with `MutableInteractionSource` or proper bounded shape clipping so that no default grey box ripple highlight appears on tap/hold.
2. **Locked Home Previews**: 
   - **For You**: Home screen preview must be a locked 2x2 collage artwork grid (4 songs max). Carousel is reserved for child screens.
   - **Genre Today**: Home screen preview must be a static stacked artwork card preview (`GenreTodayCard`) with genre title and primary color subtitle.
3. **Uncontained Square Album Artwork Carousel**:
   - In child screens (e.g., For You Remix Screen, Genre Today Screen), artwork carousels must feature uncontained items with a perfect 1:1 square aspect ratio (`aspectRatio(1f)`), preventing rectangular vertical stretching.
4. **Separation of Concerns**: Keep business logic, viewmodels, and data repositories strictly separated from UI presentation composables.
