import pandas as pd
import matplotlib.pyplot as plt
import os
import glob
import math

# Get the directory where the script is located
script_dir = os.path.dirname(os.path.abspath(__file__))

# Define the output directory relative to the script directory
output_dir = os.path.join(script_dir, '..', 'output')

# Define the plots directory within the visualizer directory
plots_dir = os.path.join(script_dir, 'plots')
if not os.path.exists(plots_dir):
    os.makedirs(plots_dir)

# List all CSV files in the output directory
csv_files = glob.glob(os.path.join(output_dir, '*.csv'))

# Check if any CSV files are found
if not csv_files:
    print(f"No CSV files found in {output_dir}.")
    exit(1)

# Initialize a dictionary to hold data from each file
dataframes = {}
for csv_file in csv_files:
    # Read the CSV file
    df = pd.read_csv(csv_file)
    # Extract the workload name or identifier from the filename for labeling
    workload_name = os.path.splitext(os.path.basename(csv_file))[0]
    
    # Convert timestamps from strings to numeric values
    df['interval_start_timestamp'] = pd.to_numeric(df['interval_start_timestamp'])
    df['interval_end_timestamp'] = pd.to_numeric(df['interval_end_timestamp'])
    
    # Calculate the average timestamp for each interval
    df['timestamp'] = (df['interval_start_timestamp'] + df['interval_end_timestamp']) / 2
    
    # Convert timestamps to seconds (assuming they are in nanoseconds)
    df['timestamp'] = df['timestamp'] / 1e9  # Adjust the unit according to your data's units
    
    # Calculate hits for each method
    df['actual_hits'] = df['total_access_count'] * df['actual_accesses_dram_hit_ratio']
    df['estimated_hits'] = df['total_access_count'] * df['estimated_dram_hit_ratio']
    df['pts_hits'] = df['total_access_count'] * df['pts_dram_hit_ratio']
    
    # Store the dataframe
    dataframes[workload_name] = df

# Find the earliest timestamp among all datasets to align timelines
min_timestamp = min(df['timestamp'].iloc[0] for df in dataframes.values())

# Adjust all timestamps to start from zero
for df in dataframes.values():
    df['timestamp'] -= min_timestamp

# Determine grid size for subplots based on the number of CSV files
num_files = len(dataframes)
cols = 3  # Set the number of columns to 3 as per your request
rows = math.ceil(num_files / cols)

# Function to create subplots with multiple metrics in each subplot
def create_subplots_multiple_metrics(metrics, ylabel, title, filename):
    fig, axes = plt.subplots(rows, cols, figsize=(18, rows * 4), sharex=True, sharey=True)
    axes = axes.flatten()
    for i, (workload_name, df) in enumerate(dataframes.items()):
        ax = axes[i]
        for metric, label, style in metrics:
            ax.plot(df['timestamp'], df[metric], marker=style['marker'], linestyle=style['linestyle'], label=label)
        ax.set_title(workload_name, fontsize=10)
        ax.grid(True)
        if i % cols == 0:
            ax.set_ylabel(ylabel)
        if i >= num_files - cols:
            ax.set_xlabel('Time (s)')  # Indicate that the x-axis is time in seconds
        ax.legend(fontsize='small')
    # Hide any unused subplots
    for j in range(i + 1, len(axes)):
        fig.delaxes(axes[j])
    fig.suptitle(title, fontsize=16)
    plt.tight_layout(rect=[0, 0.03, 1, 0.95])
    plt.savefig(os.path.join(plots_dir, filename))
    plt.close()

# Define the metrics to plot together
metrics = [
    ('actual_accesses_dram_hit_ratio', 'Actual', {'marker': 'o', 'linestyle': '-'}),
    ('estimated_dram_hit_ratio', 'Estimated', {'marker': 'x', 'linestyle': '--'}),
    ('pts_dram_hit_ratio', 'PTS', {'marker': 's', 'linestyle': ':'})
]

# Create subplots for DRAM Hit Ratios with all metrics in the same plot
create_subplots_multiple_metrics(
    metrics=metrics,
    ylabel='DRAM Hit Ratio',
    title='DRAM Hit Ratios Over Time',
    filename='dram_hit_ratios_collage.png'
)

# Create subplots for Number of Pages Accessed
def create_subplots_single_metric(metric, ylabel, title, filename):
    fig, axes = plt.subplots(rows, cols, figsize=(18, rows * 4), sharex=True)
    axes = axes.flatten()
    for i, (workload_name, df) in enumerate(dataframes.items()):
        ax = axes[i]
        ax.plot(df['timestamp'], df[metric], marker='o', linestyle='-')
        ax.set_title(workload_name, fontsize=10)
        ax.grid(True)
        if i % cols == 0:
            ax.set_ylabel(ylabel)
        if i >= num_files - cols:
            ax.set_xlabel('Time (s)')
    # Hide any unused subplots
    for j in range(i + 1, len(axes)):
        fig.delaxes(axes[j])
    fig.suptitle(title, fontsize=16)
    plt.tight_layout(rect=[0, 0.03, 1, 0.95])
    plt.savefig(os.path.join(plots_dir, filename))
    plt.close()

# Plot Number of Pages Accessed
create_subplots_single_metric(
    metric='number_of_pages_accessed',
    ylabel='Number of Pages Accessed',
    title='Number of Pages Accessed Over Time',
    filename='pages_accessed_collage.png'
)

# Plot Total Access Count
create_subplots_single_metric(
    metric='total_access_count',
    ylabel='Total Access Count',
    title='Total Access Count Over Time',
    filename='total_access_count_collage.png'
)

# Plot Difference Between Actual and Estimated DRAM Hit Ratios
def create_subplots_hit_ratio_difference(ylabel, title, filename):
    fig, axes = plt.subplots(rows, cols, figsize=(18, rows * 4), sharex=True, sharey=True)
    axes = axes.flatten()
    for i, (workload_name, df) in enumerate(dataframes.items()):
        ax = axes[i]
        df['Hit Ratio Difference'] = df['actual_accesses_dram_hit_ratio'] - df['estimated_dram_hit_ratio']
        ax.plot(df['timestamp'], df['Hit Ratio Difference'], marker='o', linestyle='-')
        ax.set_title(workload_name, fontsize=10)
        ax.grid(True)
        if i % cols == 0:
            ax.set_ylabel(ylabel)
        if i >= num_files - cols:
            ax.set_xlabel('Time (s)')
    # Hide any unused subplots
    for j in range(i + 1, len(axes)):
        fig.delaxes(axes[j])
    fig.suptitle(title, fontsize=16)
    plt.tight_layout(rect=[0, 0.03, 1, 0.95])
    plt.savefig(os.path.join(plots_dir, filename))
    plt.close()

create_subplots_hit_ratio_difference(
    ylabel='Difference in DRAM Hit Ratio',
    title='Difference Between Actual and Estimated DRAM Hit Ratios Over Time',
    filename='hit_ratio_difference_collage.png'
)

# Create a collage of bar charts for overall DRAM hit ratios per workload
def create_bar_charts_collage(filename):
    fig, axes = plt.subplots(rows, cols, figsize=(18, rows * 4))
    axes = axes.flatten()
    for i, (workload_name, df) in enumerate(dataframes.items()):
        ax = axes[i]
        
        # Calculate totals for this workload
        total_accesses = df['total_access_count'].sum()
        total_actual_hits = df['actual_hits'].sum()
        total_estimated_hits = df['estimated_hits'].sum()
        total_pts_hits = df['pts_hits'].sum()
        
        # Calculate hit ratios
        actual_ratio = total_actual_hits / total_accesses
        estimated_ratio = total_estimated_hits / total_accesses
        pts_ratio = total_pts_hits / total_accesses
        
        # Prepare data for the bar chart
        methods = ['Actual', 'Estimated', 'PTS']
        hit_ratios = [actual_ratio, estimated_ratio, pts_ratio]
        colors = ['blue', 'orange', 'green']
        
        # Create the bar chart
        bars = ax.bar(methods, hit_ratios, color=colors)
        ax.set_ylim(0, 1)
        ax.set_title(workload_name, fontsize=10)
        ax.set_ylabel('DRAM Hit Ratio')
        ax.grid(axis='y')
        
        # Annotate bars with values
        for bar, ratio in zip(bars, hit_ratios):
            yval = bar.get_height()
            ax.text(bar.get_x() + bar.get_width()/2.0, yval + 0.02, f'{ratio:.3f}', ha='center', va='bottom', fontsize=8)
        
        # Remove x-axis labels to reduce clutter
        ax.set_xticklabels(methods, rotation=0, fontsize=8)
    
    # Hide any unused subplots
    for j in range(i + 1, len(axes)):
        fig.delaxes(axes[j])
    
    fig.suptitle('Overall DRAM Hit Ratios Per Workload', fontsize=16)
    plt.tight_layout(rect=[0, 0.03, 1, 0.95])
    plt.savefig(os.path.join(plots_dir, filename))
    plt.close()

# Call the function to create the collage of bar charts
create_bar_charts_collage(filename='overall_dram_hit_ratios_collage.png')
