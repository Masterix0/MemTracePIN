import pandas as pd
import matplotlib.pyplot as plt
import os

# Replace 'your_output.csv' with the path to your CSV file
csv_file = '../output/603_bwaves_s-100-20.00-0.10.csv'  # Update this with your actual CSV file path

# Read the CSV file
df = pd.read_csv(csv_file)

# Create an 'Interval' column for plotting purposes
df['Interval'] = range(1, len(df) + 1)

# Create an output directory for the plots if it doesn't exist
output_dir = 'plots'
if not os.path.exists(output_dir):
    os.makedirs(output_dir)

# Plot DRAM Hit Ratios Over Intervals
plt.figure(figsize=(12, 6))
plt.plot(df['Interval'], df['actual_accesses_dram_hit_ratio'], marker='o', label='Actual DRAM Hit Ratio')
plt.plot(df['Interval'], df['estimated_dram_hit_ratio'], marker='x', label='Estimated DRAM Hit Ratio')
plt.plot(df['Interval'], df['pts_dram_hit_ratio'], marker='s', label='PTS DRAM Hit Ratio')
plt.xlabel('Interval')
plt.ylabel('DRAM Hit Ratio')
plt.title('DRAM Hit Ratios Over Intervals')
plt.legend()
plt.grid(True)
plt.tight_layout()
# Save the plot to a file
plt.savefig(os.path.join(output_dir, 'dram_hit_ratios.png'))
plt.close()  # Close the figure

# Plot Number of Pages Accessed Over Intervals
plt.figure(figsize=(12, 6))
plt.plot(df['Interval'], df['number_of_pages_accessed'], marker='o', color='green')
plt.xlabel('Interval')
plt.ylabel('Number of Pages Accessed')
plt.title('Number of Pages Accessed Over Intervals')
plt.grid(True)
plt.tight_layout()
# Save the plot to a file
plt.savefig(os.path.join(output_dir, 'pages_accessed.png'))
plt.close()  # Close the figure

# Plot Total Access Count Over Intervals
plt.figure(figsize=(12, 6))
plt.plot(df['Interval'], df['total_access_count'], marker='o', color='red')
plt.xlabel('Interval')
plt.ylabel('Total Access Count')
plt.title('Total Access Count Over Intervals')
plt.grid(True)
plt.tight_layout()
# Save the plot to a file
plt.savefig(os.path.join(output_dir, 'total_access_count.png'))
plt.close()  # Close the figure

# Plot Difference Between Actual and Estimated DRAM Hit Ratios
df['Hit Ratio Difference'] = df['actual_accesses_dram_hit_ratio'] - df['estimated_dram_hit_ratio']
plt.figure(figsize=(12, 6))
plt.plot(df['Interval'], df['Hit Ratio Difference'], marker='o', color='purple')
plt.xlabel('Interval')
plt.ylabel('Difference in DRAM Hit Ratio')
plt.title('Difference Between Actual and Estimated DRAM Hit Ratios Over Intervals')
plt.grid(True)
plt.tight_layout()
# Save the plot to a file
plt.savefig(os.path.join(output_dir, 'hit_ratio_difference.png'))
plt.close()  # Close the figure
